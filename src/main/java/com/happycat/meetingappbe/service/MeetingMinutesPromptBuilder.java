package com.happycat.meetingappbe.service;

import com.happycat.meetingappbe.entity.MeetingTranscriptSegment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class MeetingMinutesPromptBuilder {
    public String transcriptText(List<MeetingTranscriptSegment> segments) {
        StringBuilder result = new StringBuilder();
        segments.forEach(segment -> result.append(segmentLine(segment)));
        return result.toString();
    }

    public String segmentLine(MeetingTranscriptSegment segment) {
        String speaker = speakerName(segment);
        Instant timestamp = segment.getClientCreatedAt() != null
                ? segment.getClientCreatedAt()
                : segment.getCreatedAt();
        return "[" + timestamp + "] " + speaker + ": " + segment.getText() + "\n";
    }

    public String finalMinutesPrompt(String transcript) {
        return """
                Bạn là trợ lý tạo biên bản cuộc họp từ transcript.
                Chỉ dựa trên nội dung transcript được cung cấp, không tự bịa thông tin.
                Trả về Markdown tiếng Việt với đúng các mục sau:
                # Biên bản cuộc họp
                ## Tóm tắt
                ## Nội dung chính
                ## Quyết định
                ## Việc cần làm

                Nếu không có quyết định hoặc việc cần làm rõ ràng, ghi rõ "Không có ... rõ ràng được ghi nhận."

                Transcript:
                %s
                """.formatted(transcript);
    }

    public String chunkSummaryPrompt(String transcriptChunk) {
        return """
                Tóm tắt phần transcript cuộc họp dưới đây để phục vụ tạo biên bản cuối cùng.
                Giữ lại các ý chính, quyết định, người được nhắc tới, và việc cần làm nếu có.
                Không tự bịa thông tin.

                Transcript chunk:
                %s
                """.formatted(transcriptChunk);
    }

    public String finalMinutesFromSummariesPrompt(String summaries) {
        return """
                Bạn là trợ lý tạo biên bản cuộc họp từ các tóm tắt transcript theo từng phần.
                Chỉ dựa trên nội dung được cung cấp, không tự bịa thông tin.
                Trả về Markdown tiếng Việt với đúng các mục sau:
                # Biên bản cuộc họp
                ## Tóm tắt
                ## Nội dung chính
                ## Quyết định
                ## Việc cần làm

                Nếu không có quyết định hoặc việc cần làm rõ ràng, ghi rõ "Không có ... rõ ràng được ghi nhận."

                Tóm tắt các phần:
                %s
                """.formatted(summaries);
    }

    public int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return estimateTokens(text.length());
    }

    public int estimateTokens(int characterCount) {
        return (int) Math.ceil(characterCount / 3.0);
    }

    private String speakerName(MeetingTranscriptSegment segment) {
        if (segment.getUser() == null) {
            return "Unknown";
        }
        String firstName = segment.getUser().getFirstName();
        String lastName = segment.getUser().getLastName();
        String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (segment.getUser().getUsername() != null && !segment.getUser().getUsername().isBlank()) {
            return segment.getUser().getUsername();
        }
        return segment.getUser().getId();
    }
}
