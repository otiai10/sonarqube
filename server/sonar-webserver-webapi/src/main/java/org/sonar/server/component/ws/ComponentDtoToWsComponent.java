/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.component.ws;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.project.Visibility;
import org.sonarqube.ws.Components;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.emptyToNull;
import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.DateUtils.formatDateTime;

class ComponentDtoToWsComponent {

  /**
   * The concept of "visibility" will only be configured for these qualifiers.
   */
  private static final Set<String> QUALIFIERS_WITH_VISIBILITY = ImmutableSet.of(Qualifiers.PROJECT, Qualifiers.VIEW, Qualifiers.APP);

  private ComponentDtoToWsComponent() {
    // prevent instantiation
  }

  static Components.Component.Builder projectOrAppToWsComponent(ProjectDto project, ComponentDto component, OrganizationDto organizationDto,
    @Nullable SnapshotDto lastAnalysis) {

    checkArgument(
      Objects.equals(project.getOrganizationUuid(), organizationDto.getUuid()),
      "OrganizationUuid (%s) of ComponentDto to convert to Ws Component is not the same as the one (%s) of the specified OrganizationDto",
      project.getOrganizationUuid(), organizationDto.getUuid());

    Components.Component.Builder wsComponent = Components.Component.newBuilder()
      .setOrganization(organizationDto.getKey())
      .setKey(project.getKey())
      // TODO switch to using ProjectDto.getName() after fixing
      // org.sonarqube.tests.project.ProjectInfoTest.project_name_and_description_should_be_truncated_if_too_long
      .setName(component.name())
      .setQualifier(project.getQualifier());

    // TODO switch to using ProjectDto.getDescription() after fixing
    // org.sonarqube.tests.project.ProjectInfoTest.project_name_and_description_should_be_truncated_if_too_long
    ofNullable(emptyToNull(component.description())).ifPresent(wsComponent::setDescription);
    ofNullable(emptyToNull(component.language())).ifPresent(wsComponent::setLanguage);
    ofNullable(lastAnalysis).ifPresent(
      analysis -> {
        wsComponent.setAnalysisDate(formatDateTime(analysis.getCreatedAt()));
        ofNullable(analysis.getPeriodDate()).ifPresent(leak -> wsComponent.setLeakPeriodDate(formatDateTime(leak)));
        ofNullable(analysis.getProjectVersion()).ifPresent(wsComponent::setVersion);
      });
    if (QUALIFIERS_WITH_VISIBILITY.contains(project.getQualifier())) {
      // TODO switch to using ProjectDto.isPrivate() instead after fixing UpdateVisibilityAction WS
      wsComponent.setVisibility(Visibility.getLabel(component.isPrivate()));
      if (Qualifiers.PROJECT.equals(project.getQualifier())) {
        wsComponent.getTagsBuilder().addAllTags(project.getTags());
      }
    }

    return wsComponent;
  }

  static Components.Component.Builder componentDtoToWsComponent(ComponentDto dto, OrganizationDto organizationDto, @Nullable SnapshotDto lastAnalysis) {
    checkArgument(
      Objects.equals(dto.getOrganizationUuid(), organizationDto.getUuid()),
      "OrganizationUuid (%s) of ComponentDto to convert to Ws Component is not the same as the one (%s) of the specified OrganizationDto",
      dto.getOrganizationUuid(), organizationDto.getUuid());
    return componentDtoToWsComponent(dto, organizationDto.getKey(), lastAnalysis);
  }

  private static Components.Component.Builder componentDtoToWsComponent(ComponentDto dto, String organizationDtoKey, @Nullable SnapshotDto lastAnalysis) {
    Components.Component.Builder wsComponent = Components.Component.newBuilder()
      .setOrganization(organizationDtoKey)
      .setKey(dto.getKey())
      .setName(dto.name())
      .setQualifier(dto.qualifier());
    ofNullable(emptyToNull(dto.getBranch())).ifPresent(wsComponent::setBranch);
    ofNullable(emptyToNull(dto.getPullRequest())).ifPresent(wsComponent::setPullRequest);
    ofNullable(emptyToNull(dto.path())).ifPresent(wsComponent::setPath);
    ofNullable(emptyToNull(dto.description())).ifPresent(wsComponent::setDescription);
    ofNullable(emptyToNull(dto.language())).ifPresent(wsComponent::setLanguage);
    ofNullable(lastAnalysis).ifPresent(
      analysis -> {
        wsComponent.setAnalysisDate(formatDateTime(analysis.getCreatedAt()));
        ofNullable(analysis.getPeriodDate()).ifPresent(leak -> wsComponent.setLeakPeriodDate(formatDateTime(leak)));
        ofNullable(analysis.getProjectVersion()).ifPresent(wsComponent::setVersion);
      });
    if (QUALIFIERS_WITH_VISIBILITY.contains(dto.qualifier())) {
      wsComponent.setVisibility(Visibility.getLabel(dto.isPrivate()));
    }
    return wsComponent;
  }
}
