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

import org.jboss.interceptor.proxy.InterceptorProxyCreatorImpl;
import org.jboss.interceptor.proxy.InterceptorProxyCreator;
import org.jboss.interceptor.proxy.SimpleInterceptionHandlerFactory;
import org.jboss.interceptor.registry.InterceptorRegistry;
import org.jboss.interceptor.model.InterceptionModelImpl;
import org.jboss.interceptor.model.InterceptionType;
import org.jboss.interceptor.utils.InterceptionUtils;
import org.junit.Test;
import org.junit.Before;
import org.junit.Assert;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 */
public class InterceptionTest {
    private static final String TEAM_NAME = "Ajax Amsterdam";

    private String[] expectedLoggedValues = {
    "org.jboss.interceptors.InterceptionTest$MyFirstInterceptor_postConstruct",
            "org.jboss.interceptors.InterceptionTest$MyFirstInterceptor_aroundInvokeBefore",
            "org.jboss.interceptors.InterceptionTest$MySecondInterceptor_aroundInvokeBefore",
            "org.jboss.interceptors.FootballTeam_aroundInvokeBefore",
            "org.jboss.interceptors.FootballTeam_getName",
            "org.jboss.interceptors.FootballTeam_aroundInvokeAfter",
            "org.jboss.interceptors.InterceptionTest$MySecondInterceptor_aroundInvokeAfter",
            "org.jboss.interceptors.InterceptionTest$MyFirstInterceptor_aroundInvokeAfter",
            "org.jboss.interceptors.InterceptionTest$MySecondInterceptor_preDestroy"

    };

    @Before
    public void resetLog() {
        InterceptorTestLogger.reset();
    }

    @Test
    public void testInterception() throws Exception {

        InterceptorRegistry<Class> interceptorRegistry = new InterceptorRegistry<Class>();
        InterceptionModelImpl interceptionModel = new InterceptionModelImpl();
        interceptionModel.setInterceptors(InterceptionType.AROUND_INVOKE, FootballTeam.class.getMethod("getName"), Arrays.asList(new Class<?>[]{MyFirstInterceptor.class, MySecondInterceptor.class, FootballTeam.class}));
        interceptionModel.setInterceptors(InterceptionType.POST_CONSTRUCT, null, Arrays.asList(new Class<?>[]{MyFirstInterceptor.class}));
        interceptionModel.setInterceptors(InterceptionType.PRE_DESTROY, null, Arrays.asList(new Class<?>[]{MySecondInterceptor.class}));
        interceptorRegistry.registerInterceptionModel(FootballTeam.class, interceptionModel);

        InterceptorProxyCreator interceptorProxyCreator = new InterceptorProxyCreatorImpl(interceptorRegistry, new SimpleInterceptionHandlerFactory());
        //SoccerTeam proxy = interceptorProxyCreator.createInstrumentedInstance(FootballTeam.class, new Class<?>[]{String.class}, new Object[]{"Poli Timisoara"});
        FootballTeam proxy = interceptorProxyCreator.createProxyFromInstance(new FootballTeam(TEAM_NAME), FootballTeam.class);
        InterceptionUtils.executePostConstruct(proxy);
        Assert.assertEquals(TEAM_NAME, proxy.getName());
        InterceptionUtils.executePredestroy(proxy);
        Assert.assertArrayEquals(expectedLoggedValues, InterceptorTestLogger.getLog().toArray());

    }


    public static class MyFirstInterceptor {

        @AroundInvoke
        public Object doAround2(InvocationContext invocationContext) throws Exception {
            InterceptorTestLogger.add(MyFirstInterceptor.class,"aroundInvokeBefore");
            Object result = invocationContext.proceed();
            InterceptorTestLogger.add(MyFirstInterceptor.class, "aroundInvokeAfter");
            return result;
        }

        @PostConstruct
        public Object doAfterConstruction(InvocationContext invocationContext) throws Exception {
            InterceptorTestLogger.add(MyFirstInterceptor.class, "postConstruct");
            return invocationContext.proceed();
        }
    }

    public static class MySecondInterceptor extends MyFirstInterceptor {
        @AroundInvoke
        public Object doAround(InvocationContext invocationContext) throws Exception {
            InterceptorTestLogger.add(MySecondInterceptor.class, "aroundInvokeBefore");
            Object result = invocationContext.proceed();
            InterceptorTestLogger.add(MySecondInterceptor.class, "aroundInvokeAfter");
            return result;
        }

        @PreDestroy
        public Object doneHere(InvocationContext invocationContext) throws Exception {
            InterceptorTestLogger.add(MySecondInterceptor.class, "preDestroy");
            return invocationContext.proceed();
        }
    }
}

