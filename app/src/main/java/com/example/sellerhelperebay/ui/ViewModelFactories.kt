package com.example.sellerhelperebay.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.sellerhelperebay.SellerHelperApp
import com.example.sellerhelperebay.data.AppContainer

/** Pulls the [AppContainer] out of CreationExtras inside viewModelFactory initializers. */
fun CreationExtras.appContainer(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SellerHelperApp).container
