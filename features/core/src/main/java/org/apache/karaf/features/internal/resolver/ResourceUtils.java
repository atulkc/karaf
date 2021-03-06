/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.features.internal.resolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.utils.version.VersionRange;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.service.repository.ContentNamespace.CAPABILITY_URL_ATTRIBUTE;
import static org.osgi.service.repository.ContentNamespace.CONTENT_NAMESPACE;

public class ResourceUtils {

    public static final String TYPE_SUBSYSTEM = "karaf.subsystem";

    public static final String TYPE_FEATURE = "karaf.feature";

    public static String getUri(Resource resource) {
        List<Capability> caps = resource.getCapabilities(null);
        for (Capability cap : caps) {
            if (cap.getNamespace().equals(CONTENT_NAMESPACE)) {
                return cap.getAttributes().get(CAPABILITY_URL_ATTRIBUTE).toString();
            }
        }
        return null;
    }

    public static String getFeatureId(Resource resource) {
        List<Capability> caps = resource.getCapabilities(null);
        for (Capability cap : caps) {
            if (cap.getNamespace().equals(IDENTITY_NAMESPACE)) {
                Map<String, Object> attributes = cap.getAttributes();
                if (TYPE_FEATURE.equals(attributes.get(CAPABILITY_TYPE_ATTRIBUTE))) {
                    String name = (String) attributes.get(IDENTITY_NAMESPACE);
                    Version version = (Version) attributes.get(CAPABILITY_VERSION_ATTRIBUTE);
                    return version != null ? name + "/" + version : name;
                }
            }
        }
        return null;
    }

    public static void addIdentityRequirement(ResourceImpl resource, String name, String type, String range) {
        Map<String, String> dirs = new HashMap<String, String>();
        Map<String, Object> attrs = new HashMap<String, Object>();
        if (name != null) {
            attrs.put(IDENTITY_NAMESPACE, name);
        }
        if (type != null) {
            attrs.put(CAPABILITY_TYPE_ATTRIBUTE, type);
        }
        if (range != null) {
            attrs.put(CAPABILITY_VERSION_ATTRIBUTE, new VersionRange(range));
        }
        resource.addRequirement(new RequirementImpl(resource, IDENTITY_NAMESPACE, dirs, attrs));
    }

    public static void addIdentityRequirement(ResourceImpl resource, Resource required) {
        addIdentityRequirement(resource, required, true);
    }

    public static void addIdentityRequirement(ResourceImpl resource, Resource required, boolean mandatory) {
        for (Capability cap : required.getCapabilities(null)) {
            if (cap.getNamespace().equals(IDENTITY_NAMESPACE)) {
                Map<String, Object> attributes = cap.getAttributes();
                Map<String, String> dirs = new HashMap<String, String>();
                dirs.put(Constants.RESOLUTION_DIRECTIVE, mandatory ? Constants.RESOLUTION_MANDATORY : Constants.RESOLUTION_OPTIONAL);
                Map<String, Object> attrs = new HashMap<String, Object>();
                attrs.put(IDENTITY_NAMESPACE, attributes.get(IDENTITY_NAMESPACE));
                attrs.put(CAPABILITY_TYPE_ATTRIBUTE, attributes.get(CAPABILITY_TYPE_ATTRIBUTE));
                Version version = (Version) attributes.get(CAPABILITY_VERSION_ATTRIBUTE);
                if (version != null) {
                    attrs.put(CAPABILITY_VERSION_ATTRIBUTE, new VersionRange(version, true));
                }
                resource.addRequirement(new RequirementImpl(resource, IDENTITY_NAMESPACE, dirs, attrs));
            }
        }
    }

}
