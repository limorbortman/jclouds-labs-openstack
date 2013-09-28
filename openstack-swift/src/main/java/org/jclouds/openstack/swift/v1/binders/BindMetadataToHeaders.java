/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.openstack.swift.v1.binders;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;

import org.jclouds.http.HttpRequest;
import org.jclouds.rest.Binder;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;

/**
 * Will lower-case header keys due to a swift implementation to return headers
 * in a different case than sent. ex.
 * 
 * <pre>
 * >> X-Account-Meta-MyDelete1: foo
 * >> X-Account-Meta-MyDelete2: bar
 * </pre>
 * 
 * results in:
 * 
 * <pre>
 * << X-Account-Meta-Mydelete1: foo
 * << X-Account-Meta-Mydelete2: bar
 * </pre>
 * 
 * <h4>Note</h4> <br/>
 * HTTP response headers keys are known to be case-insensitive, but this
 * practice of mixing up case will prevent metadata keys such as those in
 * Turkish from working.
 * 
 * @see <a
 *      href="http://docs.openstack.org/api/openstack-object-storage/1.0/content/create-update-account-metadata.html">documentation</a>
 */
public abstract class BindMetadataToHeaders implements Binder {

   public static class BindAccountMetadataToHeaders extends BindMetadataToHeaders {
      BindAccountMetadataToHeaders() {
         super("x-account-meta-");
      }
   }

   public static class BindRemoveAccountMetadataToHeaders extends BindMetadataToHeaders.ForRemoval {
      BindRemoveAccountMetadataToHeaders() {
         super("x-account-meta-");
      }
   }

   public static class BindContainerMetadataToHeaders extends BindMetadataToHeaders {
      BindContainerMetadataToHeaders() {
         super("x-container-meta-");
      }
   }

   public static class BindRemoveContainerMetadataToHeaders extends BindMetadataToHeaders.ForRemoval {
      BindRemoveContainerMetadataToHeaders() {
         super("x-container-meta-");
      }
   }

   public static class BindObjectMetadataToHeaders extends BindMetadataToHeaders {
      BindObjectMetadataToHeaders() {
         super("x-object-meta-");
      }
   }

   public static class BindRemoveObjectMetadataToHeaders extends BindMetadataToHeaders.ForRemoval {
      BindRemoveObjectMetadataToHeaders() {
         super("x-object-meta-");
      }
   }

   /**
    * @see <a
    *      href="http://docs.openstack.org/api/openstack-object-storage/1.0/content/delete-account-metadata.html">documentation</a>
    */
   public static abstract class ForRemoval extends BindMetadataToHeaders {
      ForRemoval(String metadataPrefix) {
         super(metadataPrefix);
      }

      @Override
      protected void putMetadata(Builder<String, String> headers, String key, String value) {
         headers.put(String.format("x-remove%s", key.substring(1)), "ignored");
      }
   }

   private final String metadataPrefix;

   BindMetadataToHeaders(String metadataPrefix) {
      this.metadataPrefix = metadataPrefix;
   }

   @SuppressWarnings("unchecked")
   @Override
   public <R extends HttpRequest> R bindToRequest(R request, Object input) {
      checkNotNull(request, "request");
      checkArgument(input instanceof Map<?, ?>, "input must be a non-null java.util.Map!");
      Map<String, String> metadata = Map.class.cast(input);
      Builder<String, String> headers = ImmutableMultimap.<String, String> builder();
      for (Entry<String, String> keyVal : metadata.entrySet()) {
         String keyInLowercase = keyVal.getKey().toLowerCase();
         if (keyVal.getKey().startsWith(metadataPrefix)) {
            putMetadata(headers, keyInLowercase, keyVal.getValue());
         } else {
            putMetadata(headers, String.format("%s%s", metadataPrefix, keyInLowercase), keyVal.getValue());
         }
      }
      return (R) request.toBuilder().replaceHeaders(headers.build()).build();
   }

   protected void putMetadata(Builder<String, String> headers, String key, String value) {
      headers.put(key, value);
   }

}
