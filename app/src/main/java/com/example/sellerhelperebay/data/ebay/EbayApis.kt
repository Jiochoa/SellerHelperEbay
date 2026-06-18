package com.example.sellerhelperebay.data.ebay

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface EbayAuthApi {
    @FormUrlEncoded
    @POST("identity/v1/oauth2/token")
    suspend fun exchangeAuthorizationCode(
        @Header("Authorization") basicAuth: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String
    ): Response<TokenResponse>

    @FormUrlEncoded
    @POST("identity/v1/oauth2/token")
    suspend fun refreshAccessToken(
        @Header("Authorization") basicAuth: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Field("scope") scope: String
    ): Response<TokenResponse>
}

interface EbayListingApi {
    @POST("sell/listing/v1_beta/item_draft/")
    suspend fun createItemDraft(
        @Header("Authorization") bearerAuth: String,
        @Header("X-EBAY-C-MARKETPLACE-ID") marketplaceId: String,
        @Body draft: ItemDraftRequest
    ): Response<ItemDraftResponse>
}
