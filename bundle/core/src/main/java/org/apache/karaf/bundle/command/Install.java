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
package org.apache.karaf.bundle.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.MultiException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.startlevel.BundleStartLevel;

@Command(scope = "bundle", name = "install", description = "Installs one or more bundles.")
@Service
public class Install implements Action {

    @Argument(index = 0, name = "urls", description = "Bundle URLs separated by whitespaces", required = true, multiValued = true)
    List<String> urls;

    @Option(name = "-s", aliases={"--start"}, description="Starts the bundles after installation", required = false, multiValued = false)
    boolean start;

    @Option(name = "-l", aliases={"--start-level"}, description="Sets the start level of the bundles", required = false, multiValued = false)
    Integer level;
    
    @Option(name = "--force", aliases = {"-f"}, description = "Forces the command to execute", required = false, multiValued = false)
    boolean force;
    
    @Reference
    Session session;
    
    @Reference
    BundleContext bundleContext;

    @Override
    public Object execute() throws Exception {
        // install the bundles
        List<Exception> exceptions = new ArrayList<Exception>();
        List<Bundle> bundles = new ArrayList<Bundle>();
        for (String url : urls) {
            try {
                bundles.add(bundleContext.installBundle(url, null));
            } catch (Exception e) {
                exceptions.add(new Exception("Unable to install bundle " + url, e));
            }
        }
        
        // optionally set the start level of the bundles
        if (level != null) {
            if (level < 50 && !force){
                for (;;) {
                    String msg = "You are about to designate bundle as a system bundle.  Do you still wish to set the start level (yes/no): ";
                    String str = session.readLine(msg, null);
                    if ("yes".equalsIgnoreCase(str)) {
                        setStartLevel(bundles);    
                        break;
                    } else if ("no".equalsIgnoreCase(str)) {
                        break;
                    }
                }                
            } else {
                setStartLevel(bundles);    
            }
        }
        
        // optionally start the bundles
        if (start) {
            for (Bundle bundle : bundles) {
                try {
                    bundle.start();
                } catch (Exception e) {
                    exceptions.add(new Exception("Unable to start bundle " + bundle.getLocation(), e));
                }
            }
        }
        
        // print the installed bundles
        if (bundles.size() == 1) {
            System.out.println("Bundle ID: " + bundles.get(0).getBundleId());
        } else {
            StringBuffer sb = new StringBuffer("Bundle IDs: ");
            for (Bundle bundle : bundles) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(bundle.getBundleId());
            }
            System.out.println(sb);
        }
        MultiException.throwIf("Error installing bundles", exceptions);
        return null;
    }

    private void setStartLevel(List<Bundle> bundles) {
        for (Bundle bundle : bundles) {
            BundleStartLevel bsl = bundle.adapt(BundleStartLevel.class);
            if (bsl == null) {
                System.out.println("StartLevel service is unavailable for bundle id " + bundle);
                return;
            }
            bsl.setStartLevel(level);
        }
    }

}
