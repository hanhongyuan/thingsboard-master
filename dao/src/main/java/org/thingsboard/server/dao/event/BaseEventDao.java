/**
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.event;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.utils.UUIDs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.AbstractSearchTimeDao;
import org.thingsboard.server.dao.model.EventEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_BY_ID_VIEW_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_BY_TYPE_AND_ID_VIEW_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_COLUMN_FAMILY_NAME;

@Component
@Slf4j
public class BaseEventDao extends AbstractSearchTimeDao<EventEntity> implements EventDao {

    @Override
    protected Class<EventEntity> getColumnFamilyClass() {
        return EventEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return EVENT_COLUMN_FAMILY_NAME;
    }

    @Override
    public EventEntity save(Event event) {
        log.debug("Save event [{}] ", event);
        return save(new EventEntity(event), false).orElse(null);
    }

    @Override
    public Optional<EventEntity> saveIfNotExists(Event event) {
        return save(new EventEntity(event), true);
    }

    @Override
    public EventEntity findEvent(UUID tenantId, EntityId entityId, String eventType, String eventUid) {
        log.debug("Search event entity by [{}][{}][{}][{}]", tenantId, entityId, eventType, eventUid);
        Select.Where query = select().from(getColumnFamilyName()).where(
                eq(ModelConstants.EVENT_TENANT_ID_PROPERTY, tenantId))
                .and(eq(ModelConstants.EVENT_ENTITY_TYPE_PROPERTY, entityId.getEntityType()))
                .and(eq(ModelConstants.EVENT_ENTITY_ID_PROPERTY, entityId.getId()))
                .and(eq(ModelConstants.EVENT_TYPE_PROPERTY, eventType))
                .and(eq(ModelConstants.EVENT_UID_PROPERTY, eventUid));
        log.trace("Execute query [{}]", query);
        EventEntity entity = findOneByStatement(query);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}] for event entity [{}]", entity != null, entity);
        } else {
            log.debug("Search result: [{}]", entity != null);
        }
        return entity;
    }

    @Override
    public List<EventEntity> findEvents(UUID tenantId, EntityId entityId, TimePageLink pageLink) {
        log.trace("Try to find events by tenant [{}], entity [{}]and pageLink [{}]", tenantId, entityId, pageLink);
        List<EventEntity> entities = findPageWithTimeSearch(EVENT_BY_ID_VIEW_NAME,
                Arrays.asList(eq(ModelConstants.EVENT_TENANT_ID_PROPERTY, tenantId),
                        eq(ModelConstants.EVENT_ENTITY_TYPE_PROPERTY, entityId.getEntityType()),
                        eq(ModelConstants.EVENT_ENTITY_ID_PROPERTY, entityId.getId())),
                pageLink);
        log.trace("Found events by tenant [{}], entity [{}] and pageLink [{}]", tenantId, entityId, pageLink);
        return entities;
    }

    @Override
    public List<EventEntity> findEvents(UUID tenantId, EntityId entityId, String eventType, TimePageLink pageLink) {
        log.trace("Try to find events by tenant [{}], entity [{}], type [{}] and pageLink [{}]", tenantId, entityId, eventType, pageLink);
        List<EventEntity> entities = findPageWithTimeSearch(EVENT_BY_TYPE_AND_ID_VIEW_NAME,
                Arrays.asList(eq(ModelConstants.EVENT_TENANT_ID_PROPERTY, tenantId),
                        eq(ModelConstants.EVENT_ENTITY_TYPE_PROPERTY, entityId.getEntityType()),
                        eq(ModelConstants.EVENT_ENTITY_ID_PROPERTY, entityId.getId()),
                        eq(ModelConstants.EVENT_TYPE_PROPERTY, eventType)),
                pageLink.isAscOrder() ? QueryBuilder.asc(ModelConstants.EVENT_TYPE_PROPERTY) :
                        QueryBuilder.desc(ModelConstants.EVENT_TYPE_PROPERTY),
                pageLink);
        log.trace("Found events by tenant [{}], entity [{}], type [{}] and pageLink [{}]", tenantId, entityId, eventType, pageLink);
        return entities;
    }

    private Optional<EventEntity> save(EventEntity entity, boolean ifNotExists) {
        if (entity.getId() == null) {
            entity.setId(UUIDs.timeBased());
        }
        Insert insert = QueryBuilder.insertInto(getColumnFamilyName())
                .value(ModelConstants.ID_PROPERTY, entity.getId())
                .value(ModelConstants.EVENT_TENANT_ID_PROPERTY, entity.getTenantId())
                .value(ModelConstants.EVENT_ENTITY_TYPE_PROPERTY, entity.getEntityType())
                .value(ModelConstants.EVENT_ENTITY_ID_PROPERTY, entity.getEntityId())
                .value(ModelConstants.EVENT_TYPE_PROPERTY, entity.getEventType())
                .value(ModelConstants.EVENT_UID_PROPERTY, entity.getEventUId())
                .value(ModelConstants.EVENT_BODY_PROPERTY, entity.getBody());
        if (ifNotExists) {
            insert = insert.ifNotExists();
        }
        ResultSet rs = executeWrite(insert);
        if (rs.wasApplied()) {
            return Optional.of(entity);
        } else {
            return Optional.empty();
        }
    }
}
