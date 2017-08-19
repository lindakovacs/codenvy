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
package com.codenvy.plugin.webhooks.vsts.shared;

import org.eclipse.che.dto.shared.DTO;

@DTO
public interface PullRequestUpdatedEvent extends GenericEvent {

  /**
   * Get event resource.
   *
   * @return {@link PullRequestUpdatedResource} resource
   */
  PullRequestUpdatedResource getResource();

  void setResource(final PullRequestUpdatedResource resource);

  PullRequestUpdatedEvent withResource(final PullRequestUpdatedResource resource);
}
