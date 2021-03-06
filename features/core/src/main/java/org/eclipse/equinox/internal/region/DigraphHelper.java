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
package org.eclipse.equinox.internal.region;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.features.internal.service.FeaturesServiceImpl;
import org.apache.karaf.features.internal.util.JsonReader;
import org.apache.karaf.features.internal.util.JsonWriter;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.hooks.bundle.CollisionHook;

public class DigraphHelper {

    private static final String DIGRAPH_FILE = "digraph.json";

    private static final String REGIONS = "regions";
    private static final String EDGES = "edges";
    private static final String TAIL = "tail";
    private static final String HEAD = "head";
    private static final String POLICY = "policy";

    public static StandardRegionDigraph loadDigraph(BundleContext bundleContext) throws BundleException, IOException, InvalidSyntaxException {
        StandardRegionDigraph digraph;
        ThreadLocal<Region> threadLocal = new ThreadLocal<Region>();
        File digraphFile = bundleContext.getDataFile(DIGRAPH_FILE);
        if (digraphFile == null || !digraphFile.exists()) {
            digraph = new StandardRegionDigraph(bundleContext, threadLocal);
            Region root = digraph.createRegion(FeaturesServiceImpl.ROOT_REGION);
            for (Bundle bundle : bundleContext.getBundles()) {
                root.addBundle(bundle);
            }
        } else {
            InputStream in = new FileInputStream(digraphFile);
            try {
                digraph = readDigraph(new DataInputStream(in), bundleContext, threadLocal);
            } finally {
                in.close();
            }
        }
        return digraph;
    }

    public static void saveDigraph(BundleContext bundleContext, RegionDigraph digraph) throws IOException {
        File digraphFile = bundleContext.getDataFile(DIGRAPH_FILE);
        FileOutputStream out = new FileOutputStream(digraphFile);
        try {
            saveDigraph(digraph, out);
        } catch (Exception e) {
            // Ignore
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                // ignore;
            }
        }
    }

    public static CollisionHook getCollisionHook(StandardRegionDigraph digraph) {
        return (CollisionHook) digraph.getBundleCollisionHook();
    }


    @SuppressWarnings("unchecked")
    static StandardRegionDigraph readDigraph(InputStream in, BundleContext bundleContext, ThreadLocal<Region> threadLocal) throws IOException, BundleException, InvalidSyntaxException {
        StandardRegionDigraph digraph = new StandardRegionDigraph(bundleContext, threadLocal);
        Map json = (Map) JsonReader.read(in);
        Map<String, Collection<Number>> regions = (Map<String, Collection<Number>>) json.get(REGIONS);
        for (Map.Entry<String, Collection<Number>> rmap : regions.entrySet()) {
            String name = rmap.getKey();
            Region region = digraph.createRegion(name);
            for (Number b : rmap.getValue()) {
                region.addBundle(b.longValue());
            }
        }
        List<Map<String, Object>> edges = (List<Map<String, Object>>) json.get(EDGES);
        for (Map<String, Object> e : edges) {
            String tail = (String) e.get(TAIL);
            String head = (String) e.get(HEAD);
            Map<String, Collection<String>> policy = (Map<String, Collection<String>>) e.get(POLICY);
            RegionFilterBuilder builder = digraph.createRegionFilterBuilder();
            for (Map.Entry<String,Collection<String>> rf : policy.entrySet()) {
                String ns = rf.getKey();
                for (String f : rf.getValue()) {
                    builder.allow(ns, f);
                }
            }
            digraph.connect(digraph.getRegion(tail), builder.build(), digraph.getRegion(head));
        }
        return digraph;
    }

    static void saveDigraph(RegionDigraph digraph, OutputStream out) throws IOException {
        Map<String, Object> json = new LinkedHashMap<String, Object>();
        Map<String, Set<Long>> regions = new LinkedHashMap<String, Set<Long>>();
        json.put(REGIONS, regions);
        for (Region region : digraph.getRegions()) {
            regions.put(region.getName(), region.getBundleIds());
        }
        List<Map<String, Object>> edges = new ArrayList<Map<String, Object>>();
        json.put(EDGES, edges);
        for (Region tail : digraph.getRegions()) {
            for (RegionDigraph.FilteredRegion fr : digraph.getEdges(tail)) {
                Map<String, Object> edge = new HashMap<String, Object>();
                edge.put(TAIL, tail.getName());
                edge.put(HEAD, fr.getRegion().getName());
                edge.put(POLICY, fr.getFilter().getSharingPolicy());
                edges.add(edge);

            }
        }
        JsonWriter.write(out, json);
    }

}
