package com.happycat.meetingappbe.service;

import com.happycat.meetingappbe.entity.MeetingMinutes;
import com.happycat.meetingappbe.entity.MeetingTranscriptSegment;
import com.happycat.meetingappbe.enums.MeetingMinutesStatus;
import com.happycat.meetingappbe.exception.AppException;
import com.happycat.meetingappbe.exception.ErrorCode;
import com.happycat.meetingappbe.repository.MeetingMinutesRepository;
import com.happycat.meetingappbe.repository.MeetingTranscriptSegmentRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeetingMinutesWorker {
    static final int DIRECT_TRANSCRIPT_TOKEN_LIMIT = 14_000;
    static final int CHUNK_TOKEN_LIMIT = 12_000;
    static final int CHUNK_SUMMARY_TOKEN_LIMIT = 2_000;
    static final int FINAL_MINUTES_TOKEN_LIMIT = 3_000;

    MeetingMinutesRepository minutesRepository;
    MeetingTranscriptSegmentRepository transcriptSegmentRepository;
    MeetingMinutesPromptBuilder promptBuilder;
    LlmChatClient llmChatClient;
    TransactionTemplate transactionTemplate;

    @Async
    public void generateAsync(String minutesId) {
        try {
            GenerationInput input = loadGenerationInput(minutesId);
            GenerationResult result = generateMarkdown(input);
            markCompleted(minutesId, result.markdown(), input.segmentCount(), result.chunkCount());
        } catch (Exception exception) {
            log.debug("Meeting minutes generation failed: minutesId={}", minutesId, exception);
            markFailed(minutesId, exception);
        }
    }

    GenerationResult generateMarkdown(GenerationInput input) {
        String markdown;
        int chunkCount;
        if (promptBuilder.estimateTokens(input.transcript()) <= DIRECT_TRANSCRIPT_TOKEN_LIMIT) {
            markdown = llmChatClient.complete(promptBuilder.finalMinutesPrompt(input.transcript()), FINAL_MINUTES_TOKEN_LIMIT);
            chunkCount = 1;
        } else {
            List<String> summaries = input.chunks().stream()
                    .map(chunk -> llmChatClient.complete(promptBuilder.chunkSummaryPrompt(chunk), CHUNK_SUMMARY_TOKEN_LIMIT))
                    .toList();
            markdown = llmChatClient.complete(
                    promptBuilder.finalMinutesFromSummariesPrompt(String.join("\n\n", summaries)),
                    FINAL_MINUTES_TOKEN_LIMIT);
            chunkCount = input.chunks().size();
        }

        return new GenerationResult(markdown, chunkCount);
    }

    GenerationInput loadGenerationInput(String minutesId) {
        return transactionTemplate.execute(status -> {
            MeetingMinutes minutes = minutesRepository.findById(minutesId)
                    .orElseThrow(() -> new AppException(ErrorCode.MEETING_MINUTES_NOT_FOUND));
            List<MeetingTranscriptSegment> segments = transcriptSegmentRepository
                    .findForMinutesByMeetingId(minutes.getMeeting().getId());
            if (segments.isEmpty()) {
                throw new AppException(ErrorCode.MEETING_MINUTES_NO_TRANSCRIPT);
            }
            return new GenerationInput(
                    promptBuilder.transcriptText(segments),
                    splitChunks(segments),
                    segments.size());
        });
    }

    private List<String> splitChunks(List<MeetingTranscriptSegment> segments) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (MeetingTranscriptSegment segment : segments) {
            String line = promptBuilder.segmentLine(segment);
            if (!current.isEmpty()
                    && promptBuilder.estimateTokens(current.length() + line.length()) > CHUNK_TOKEN_LIMIT) {
                chunks.add(current.toString());
                current = new StringBuilder();
            }
            current.append(line);
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    void markCompleted(String minutesId, String markdown, int sourceSegmentCount, int chunkCount) {
        transactionTemplate.executeWithoutResult(status -> {
            MeetingMinutes minutes = minutesRepository.findById(minutesId)
                    .orElseThrow(() -> new AppException(ErrorCode.MEETING_MINUTES_NOT_FOUND));
            minutes.setStatus(MeetingMinutesStatus.COMPLETED);
            minutes.setContentMarkdown(markdown);
            minutes.setSourceSegmentCount(sourceSegmentCount);
            minutes.setChunkCount(chunkCount);
            minutes.setFailureReason(null);
            minutesRepository.save(minutes);
        });
    }

    void markFailed(String minutesId, Exception exception) {
        transactionTemplate.executeWithoutResult(status -> {
            minutesRepository.findById(minutesId).ifPresent(minutes -> {
                minutes.setStatus(MeetingMinutesStatus.FAILED);
                minutes.setFailureReason(failureReason(exception));
                minutesRepository.save(minutes);
            });
        });
    }

    private String failureReason(Exception exception) {
        if (exception instanceof AppException appException) {
            return appException.getErrorCode().getMessage();
        }
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    record GenerationInput(String transcript, List<String> chunks, int segmentCount) {
    }

    record GenerationResult(String markdown, int chunkCount) {
    }
}
