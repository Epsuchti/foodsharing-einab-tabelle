# Implementation Notes

- Admin access is configured through `app.auth.admin-emails` in [`server/src/main/resources/application.properties`](server/src/main/resources/application.properties). The default local admin email is `admin@foodsharing.local`.
- Booking users are still uniquely identified for booking rules by the combination of `email + foodsharingId`. The passwordless user login flow uses the email address only and shows bookings for all booking-user records with that email.
- If SMTP is not configured or unavailable, login links and notification emails are logged by the backend instead of being sent.
- For local SMTP testing, start MailHog with [`server/start_smtp_server.sh`](/Users/eric/develop/foodsharing-einab-tabelle/server/start_smtp_server.sh). The local profile is configured to use `localhost:2525`.
- The initial iCal support stores the teacher link and exposes parsed candidate events through the teacher API. Parsing is intentionally lightweight and non-blocking.
