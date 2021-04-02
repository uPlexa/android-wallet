/*
 * Copyright (c) 2017 m2049r et al.
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

import com.uplexa.upxwallet.service.exchange.api.ExchangeException;
import com.uplexa.upxwallet.service.exchange.api.BaseExchangeRate;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.NoSuchElementException;

class ExchangeRateImpl extends BaseExchangeRate {

    @Override
    public String getServiceName() {
        return "coingecko.com";
    }

    @Override
    public String getBaseCurrency() {
        return baseCurrency;
    }

    @Override
    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    ExchangeRateImpl(@NonNull final String baseCurrency, @NonNull final String quoteCurrency, double rate) {
        super(baseCurrency, quoteCurrency, rate);
    }

    ExchangeRateImpl(@NonNull final String baseCurrency, @NonNull final String quoteCurrency, double rate, final boolean inverse) {
        double price = inverse ? (1d / rate) : rate;
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.rate = price;
    }
}
