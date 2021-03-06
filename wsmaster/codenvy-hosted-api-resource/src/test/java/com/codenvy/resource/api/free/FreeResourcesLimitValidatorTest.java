/*
 * Copyright (c) [2012] - [2017] Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package com.codenvy.resource.api.free;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;

import com.codenvy.resource.shared.dto.FreeResourcesLimitDto;
import com.codenvy.resource.shared.dto.ResourceDto;
import java.util.Arrays;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Test for {@link FreeResourcesLimitValidator}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class FreeResourcesLimitValidatorTest {
  @Mock private ResourceValidator resourceValidator;

  @InjectMocks private FreeResourcesLimitValidator validator;

  @Test(
    expectedExceptions = BadRequestException.class,
    expectedExceptionsMessageRegExp = "Missed free resources limit description."
  )
  public void shouldThrowBadRequestExceptionWhenFreeResourcesIsNull() throws Exception {
    //when
    validator.check(null);
  }

  @Test(
    expectedExceptions = BadRequestException.class,
    expectedExceptionsMessageRegExp = "Missed account id."
  )
  public void shouldThrowBadRequestExceptionWhenAccountIdIsMissed() throws Exception {
    //when
    validator.check(
        DtoFactory.newDto(FreeResourcesLimitDto.class)
            .withResources(
                singletonList(
                    DtoFactory.newDto(ResourceDto.class)
                        .withType("test")
                        .withUnit("mb")
                        .withAmount(1230))));
  }

  @Test(
    expectedExceptions = BadRequestException.class,
    expectedExceptionsMessageRegExp = "invalid resource"
  )
  public void shouldRethrowBadRequestExceptionWhenThereIsAnyInvalidResource() throws Exception {
    //given
    Mockito.doNothing()
        .doThrow(new BadRequestException("invalid resource"))
        .when(resourceValidator)
        .validate(any());

    //when
    validator.check(
        DtoFactory.newDto(FreeResourcesLimitDto.class)
            .withAccountId("account123")
            .withResources(
                Arrays.asList(
                    DtoFactory.newDto(ResourceDto.class)
                        .withType("test")
                        .withUnit("mb")
                        .withAmount(1230),
                    DtoFactory.newDto(ResourceDto.class)
                        .withType("test2")
                        .withUnit("mb")
                        .withAmount(3214))));
  }

  @Test(
    expectedExceptions = BadRequestException.class,
    expectedExceptionsMessageRegExp =
        "Free resources limit should contain only one resources with type 'test'."
  )
  public void
      shouldThrowBadRequestExceptionWhenAccountResourcesLimitContainTwoResourcesWithTheSameType()
          throws Exception {
    //when
    validator.check(
        DtoFactory.newDto(FreeResourcesLimitDto.class)
            .withAccountId("account123")
            .withResources(
                Arrays.asList(
                    DtoFactory.newDto(ResourceDto.class)
                        .withType("test")
                        .withUnit("mb")
                        .withAmount(1230),
                    DtoFactory.newDto(ResourceDto.class)
                        .withType("test")
                        .withUnit("mb")
                        .withAmount(3))));
  }

  @Test
  public void shouldNotThrowAnyExceptionWhenAccountResourcesLimitIsValid() throws Exception {
    //when
    validator.check(
        DtoFactory.newDto(FreeResourcesLimitDto.class)
            .withAccountId("account123")
            .withResources(
                singletonList(
                    DtoFactory.newDto(ResourceDto.class)
                        .withType("test")
                        .withUnit("mb")
                        .withAmount(1230))));
  }
}
