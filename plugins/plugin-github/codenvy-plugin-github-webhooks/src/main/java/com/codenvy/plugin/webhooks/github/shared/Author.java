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
package com.codenvy.plugin.webhooks.github.shared;

import org.eclipse.che.dto.shared.DTO;

@DTO
public interface Author {

  /**
   * Get author's name.
   *
   * @return {@link String} name
   */
  String getName();

  void setName(final String name);

  Author withName(final String name);

  /**
   * Get author's email.
   *
   * @return {@link String} email
   */
  String getEmail();

  void setEmail(final String email);

  Author withEmail(final String email);
}
