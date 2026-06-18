# eBay Integration Setup

The app connects to eBay with OAuth and creates a **draft** listing via the Sell Listing
API (`createItemDraft`). It never publishes; the user finishes the listing in the eBay app.

## Keys live in `local.properties` (never committed)

Add these keys. Sandbox and production keysets are **separate** — fill the set you use.

```properties
# SANDBOX (OAuth works here, but createItemDraft does NOT — see note below)
ebay.sandbox.clientId=YOUR_SANDBOX_APP_ID
ebay.sandbox.clientSecret=YOUR_SANDBOX_CERT_ID
ebay.sandbox.ruName=YOUR_SANDBOX_RUNAME

# PRODUCTION
ebay.prod.clientId=YOUR_PROD_APP_ID
ebay.prod.clientSecret=YOUR_PROD_CERT_ID
ebay.prod.ruName=YOUR_PROD_RUNAME

# Which environment to use: SANDBOX (default) or PROD
ebay.env=SANDBOX
```

After editing, rebuild (the values are baked into `BuildConfig` at compile time).

## eBay developer portal config (per keyset, sandbox AND production)

On the RuName ("User Tokens / Your eBay Sign-in Settings" page):

1. Set the bottom radio to **OAuth** (not Auth'n'Auth).
2. Set **"Your auth accepted URL"** to a real `https://` URL (e.g.
   `https://ochoajuan.com/ebay/accepted`). It does not need to host anything — the app
   intercepts the redirect to it and reads the `?code=` before the page loads. **It must
   not be blank**, or eBay shows its default page and never returns the code.
3. (Optional) Set a declined URL too.

The same sandbox/production *user* account works regardless of the Auth'n'Auth/OAuth
toggle in eBay's own "Get a User Token" tool — that toggle is only for eBay's tester.

## ⚠️ createItemDraft is Production-only and access-gated

Confirmed 2026-06-13: the Sell Listing API (`/sell/listing/v1_beta/item_draft/`) returns
**404 in Sandbox** even with a valid, consented token. It is a `v1_beta` limited-release
API:

- **Not available in Sandbox at all.** OAuth and the draft-building pipeline can be tested
  in sandbox, but the push itself will return 404.
- **In Production it requires eBay to grant your app access** to the Sell Listing API
  (the `sell.item.draft` OAuth scope alone is not sufficient). Request access via the eBay
  developer portal / developer support, then set `ebay.env=PROD`.

Until access is granted, the app shows a clear message instead of pushing.

## To go to production

1. Apply to eBay for Sell Listing API (`createItemDraft`) access.
2. Configure the production RuName as above (OAuth mode + accepted URL).
3. Fill `ebay.prod.*` keys in `local.properties` and set `ebay.env=PROD`.
4. Rebuild. Connecting will now create real drafts (still drafts, never published) in your
   real eBay account; finish them in the eBay app via the "Open in eBay" link.
