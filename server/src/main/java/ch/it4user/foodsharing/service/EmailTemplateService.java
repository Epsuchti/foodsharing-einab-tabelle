package ch.it4user.foodsharing.service;

import java.util.Map;
import java.util.stream.Collectors;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class EmailTemplateService {

    private static final ZoneId SWISS_ZONE = ZoneId.of("Europe/Zurich");
    private static final DateTimeFormatter SWISS_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public String render(String title, String bodyHtml) {
        return """
                <html>
                  <body style="margin:0;background:#f4f7f2;padding:24px;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                    <div style="max-width:720px;margin:0 auto;background:#ffffff;border:1px solid #d9e8d7;border-radius:16px;overflow:hidden;box-shadow:0 12px 30px rgba(15,23,42,0.08);">
                      <div style="background:#166534;color:#ffffff;padding:28px;">
                        <div style="font-size:12px;letter-spacing:.08em;text-transform:uppercase;opacity:.85;">Foodsharing EinAb</div>
                        <h1 style="margin:8px 0 0;font-size:28px;line-height:1.2;">%s</h1>
                      </div>
                      <div style="padding:28px;">%s</div>
                    </div>
                  </body>
                </html>
                """.formatted(escape(title), bodyHtml);
    }

    public String paragraph(String text) {
        return "<p style=\"margin:0 0 16px;line-height:1.6;\">" + escape(text) + "</p>";
    }

    public String paragraphHtml(String html) {
        return "<p style=\"margin:0 0 16px;line-height:1.6;\">" + html + "</p>";
    }

    public String button(String label, String url) {
        return """
                <table role="presentation" cellspacing="0" cellpadding="0" style="margin:24px 0;">
                  <tr>
                    <td>
                      <a href="%s" style="display:inline-block;background:#16a34a;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:10px;font-weight:700;">%s</a>
                    </td>
                  </tr>
                </table>
                """.formatted(escape(url), escape(label));
    }

    public String link(String label, String url) {
        return """
                <a href="%s" style="color:#166534;text-decoration:underline;">%s</a>
                """.formatted(escape(url), escape(label));
    }

    public String detailsTable(Map<String, String> fields) {
        String rows = fields.entrySet().stream()
                .map(entry -> """
                        <tr>
                          <td style="padding:10px 12px;border-bottom:1px solid #e5e7eb;font-weight:700;vertical-align:top;width:180px;">%s</td>
                          <td style="padding:10px 12px;border-bottom:1px solid #e5e7eb;vertical-align:top;white-space:pre-wrap;">%s</td>
                        </tr>
                        """.formatted(escape(entry.getKey()), escape(entry.getValue())))
                .collect(Collectors.joining());
        return """
                <table role="presentation" cellspacing="0" cellpadding="0" style="width:100%%;border-collapse:collapse;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;">
                  %s
                </table>
                """.formatted(rows);
    }

    public String note(String text) {
        return """
                <div style="margin-top:24px;padding:16px 18px;border-left:4px solid #16a34a;background:#f0fdf4;border-radius:10px;">
                  <p style="margin:0;line-height:1.6;">%s</p>
                </div>
                """.formatted(escape(text));
    }

    public String swissDateTime(OffsetDateTime value) {
        if (value == null) {
            return "";
        }
        return SWISS_DATE_TIME.format(value.atZoneSameInstant(SWISS_ZONE));
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
