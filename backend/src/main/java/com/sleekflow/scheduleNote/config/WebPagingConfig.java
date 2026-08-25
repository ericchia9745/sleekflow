package com.sleekflow.scheduleNote.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * Serialises pages as Spring Data's {@code PagedModel} rather than the internal
 * {@code PageImpl} shape, which Spring Data explicitly documents as unstable.
 * The client's generated types depend on this staying put.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
class WebPagingConfig {

}
