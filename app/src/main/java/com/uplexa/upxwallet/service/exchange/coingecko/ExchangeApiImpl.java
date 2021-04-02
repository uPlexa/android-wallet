/*
 * Copyright (c) 2017-2019 m2049r et al.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uplexa.upxwallet.service.exchange.coingecko;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.uplexa.upxwallet.model.Wallet;
import com.uplexa.upxwallet.service.exchange.api.ExchangeApi;
import com.uplexa.upxwallet.service.exchange.api.ExchangeCallback;
import com.uplexa.upxwallet.service.exchange.api.ExchangeException;
import com.uplexa.upxwallet.service.exchange.api.ExchangeRate;
import com.uplexa.upxwallet.util.Helper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class ExchangeApiImpl implements ExchangeApi {
    static final String UPX_CRYPTO_ID = "uplexa";

    @NonNull
    private final OkHttpClient okHttpClient;

    private final HttpUrl baseUrl;

    //so we can inject the mockserver url
    @VisibleForTesting
    public ExchangeApiImpl(@NonNull final OkHttpClient okHttpClient, final HttpUrl baseUrl) {
        this.okHttpClient = okHttpClient;
        this.baseUrl = baseUrl;
    }

    public ExchangeApiImpl(@NonNull final OkHttpClient okHttpClient) {
        this(okHttpClient, HttpUrl.parse("https://api.coingecko.com/api/v3/simple/price"));
    }

    @Override
    public void queryExchangeRate(@NonNull final String baseCurrency, @NonNull final String quoteCurrency,
                                  @NonNull final ExchangeCallback callback) {

        if (baseCurrency.equals(quoteCurrency)) {
            callback.onSuccess(new ExchangeRateImpl(baseCurrency, quoteCurrency, 1.0));
            return;
        }

        boolean invertQuery;

        if (Helper.BASE_CRYPTO.equals(baseCurrency)) {
            invertQuery = false;
        } else if (Helper.BASE_CRYPTO.equals(quoteCurrency)) {
            invertQuery = true;
        } else {
            callback.onError(new IllegalArgumentException("no crypto specified"));
            return;
        }

        Timber.d("queryExchangeRate: i %b, b %s, q %s", invertQuery, baseCurrency, quoteCurrency);
        final boolean invert = invertQuery;
        String symbol = invert ? quoteCurrency : baseCurrency;
        if (symbol.equals(Wallet.UPX_SYMBOL)) {
            symbol = UPX_CRYPTO_ID;
        }
        final String base = symbol;
        final String quote = invert ? baseCurrency : quoteCurrency;

        final HttpUrl url = baseUrl.newBuilder()
                .addQueryParameter("ids", base.toLowerCase())
                .addQueryParameter("vs_currencies", quote.toLowerCase())
                .build();

        final Request httpRequest = createHttpRequest(url);

        okHttpClient.newCall(httpRequest).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(final Call call, final IOException ex) {
                callback.onError(ex);
            }

            @Override
            public void onResponse(final Call call, final Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        final JSONObject json = new JSONObject(response.body().string());
                        if (json.isNull(base)) {
                            callback.onError(new ExchangeException(response.code(), "No price found"));
                            return;
                        }

                        final JSONObject price = json.getJSONObject(base);
                        final double rate = price.getDouble(quote.toLowerCase());
                        reportSuccess(baseCurrency, quoteCurrency, rate, invert, callback);
                    } catch (JSONException ex) {
                        callback.onError(new ExchangeException(ex.getLocalizedMessage()));
                    }
                } else {
                    callback.onError(new ExchangeException(response.code(), response.message()));
                }
            }
        });
    }

    void reportSuccess(@NonNull final String baseCurrency, @NonNull final String quoteCurrency, double rate, boolean inverse, ExchangeCallback callback) {
        final ExchangeRate exchangeRate = new ExchangeRateImpl(baseCurrency, quoteCurrency, rate, inverse);
        callback.onSuccess(exchangeRate);
    }

    private Request createHttpRequest(final HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .get()
                .build();
    }
}
