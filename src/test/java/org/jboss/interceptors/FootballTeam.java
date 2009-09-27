/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.interceptors;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class FootballTeam {

   private String teamName;

    // an empty-argument constructor is required for proxifycation
    public FootballTeam() {

    }

    public FootballTeam(String s) {
        this.teamName = s;
    }

    public String getName() {
        InterceptorTestLogger.add(FootballTeam.class, "getName");
        return teamName;
    }

    @AroundInvoke
    public Object itsMe(InvocationContext invocationContext) throws Exception {
        InterceptorTestLogger.add(FootballTeam.class, "aroundInvokeBefore");
        Object result = invocationContext.proceed();
        InterceptorTestLogger.add(FootballTeam.class, "aroundInvokeAfter");
        return result;
    }
}
