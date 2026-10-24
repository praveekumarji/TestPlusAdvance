# TestPlusAdvance
all in one

## Account deletion

Google Play users can request deletion of their TestPulse account and associated personal data at:

`https://<your-production-host>/account-deletion.html`

Requests are stored for administrator review. The administrator can list requests with `GET /api/account-deletion/requests` and mark one `COMPLETED` or `REJECTED` with `PATCH /api/account-deletion/requests/{id}` using an admin token. Account deletion should be completed within 30 days; records required for legal, payment, or fraud-prevention purposes may be retained where applicable.

## Google Play Billing

Razorpay configuration and fields have been removed. Payment records now accept `googlePurchaseToken`, `googleProductId`, and `googleOrderId`. The backend requires a purchase token for successful records, but production entitlement granting should also verify that token with the Google Play Developer API using a service account before trusting the `SUCCESS` status.

Set `GOOGLE_PLAY_PACKAGE_NAME`, `GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL`, and `GOOGLE_PLAY_SERVICE_ACCOUNT_PRIVATE_KEY` in the deployment environment. Grant the service account access to the app in Google Play Console with permission to view financial data and manage orders. The service currently verifies one-time products through `purchases.products`, confirms the purchase state and product ID, acknowledges the purchase, and prevents reuse of a purchase token.
