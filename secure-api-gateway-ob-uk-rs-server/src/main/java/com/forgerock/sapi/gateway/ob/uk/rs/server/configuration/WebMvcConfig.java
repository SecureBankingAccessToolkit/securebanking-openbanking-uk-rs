/*
 * Copyright © 2020-2025 ForgeRock AS (obst@forgerock.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.forgerock.sapi.gateway.ob.uk.rs.server.configuration;

import com.forgerock.sapi.gateway.ob.uk.rs.server.web.DisabledEndpointInterceptor;
import com.forgerock.sapi.gateway.uk.common.shared.spring.web.filter.FapiInteractionIdFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final DisabledEndpointInterceptor disabledEndpointInterceptor;

    public WebMvcConfig(DisabledEndpointInterceptor disabledEndpointInterceptor) {
        this.disabledEndpointInterceptor = disabledEndpointInterceptor;
    }

    @Bean
    ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    /**
     * Installs the {@link FapiInteractionIdFilter}, this filter adds the x-fapi-interaction-id header value to the
     * logging context.
     */
    @Bean
    FapiInteractionIdFilter fapInteractionIdFilter() {
        return new FapiInteractionIdFilter();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(disabledEndpointInterceptor);
    }
}
