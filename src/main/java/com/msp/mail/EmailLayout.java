package com.msp.mail;

public final class EmailLayout {
    private EmailLayout() {}

    public static String wrap(String title, String innerHtml) {
        return """
            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#F8FAFC;margin:0;padding:24px 12px">
              <tr><td align="center">
                <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;width:100%%;background:#FFFFFF;border:1px solid #E2E8F0;border-radius:12px;overflow:hidden">
                  <tr>
                    <td style="background:#4F46E5;padding:20px 28px;font-family:Inter,Arial,Helvetica,sans-serif">
                      <p style="margin:0;color:#FFFFFF;font-size:18px;font-weight:700">POSify</p>
                      <p style="margin:4px 0 0;color:#C7D2FE;font-size:12px">Retail POS</p>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:28px;font-family:Inter,Arial,Helvetica,sans-serif;color:#0F172A;font-size:15px;line-height:1.6">
                      <h1 style="margin:0 0 16px;font-size:22px;font-weight:700;color:#0F172A">%s</h1>
                      %s
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:16px 28px 24px;border-top:1px solid #E2E8F0;font-family:Inter,Arial,Helvetica,sans-serif;color:#64748B;font-size:12px;line-height:1.5">
                      If you did not expect this email, you can ignore it.<br/>
                      &copy; POSify
                    </td>
                  </tr>
                </table>
              </td></tr>
            </table>
            """.formatted(title, innerHtml);
    }

    public static String button(String href, String label) {
        return """
            <table role="presentation" cellspacing="0" cellpadding="0" style="margin:20px 0">
              <tr>
                <td bgcolor="#4F46E5" style="border-radius:8px">
                  <a href="%s" style="display:inline-block;padding:12px 20px;font-family:Inter,Arial,Helvetica,sans-serif;font-size:14px;font-weight:600;color:#FFFFFF;text-decoration:none">%s</a>
                </td>
              </tr>
            </table>
            """.formatted(href, label);
    }

    public static String code(String value) {
        return """
            <p style="margin:20px 0;text-align:center">
              <span style="display:inline-block;padding:14px 22px;border:1px solid #E2E8F0;border-radius:8px;background:#F8FAFC;font-family:'JetBrains Mono','Courier New',monospace;font-size:28px;letter-spacing:8px;font-weight:700;color:#4F46E5">%s</span>
            </p>
            """.formatted(value);
    }

    public static String muted(String text) {
        return "<p style=\"margin:16px 0 0;color:#64748B;font-size:13px\">" + text + "</p>";
    }
}
