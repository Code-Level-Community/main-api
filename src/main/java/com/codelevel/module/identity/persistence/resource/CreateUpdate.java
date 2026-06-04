package com.codelevel.module.identity.persistence.resource;

import com.codelevel.shared.exception.ApplicationException;
import com.codelevel.shared.exception.ResourceAlreadyExists;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.PersistenceException;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.slf4j.LoggerFactory;

public interface CreateUpdate {

    default void saveOrUpdate(final PanacheEntityBase entity) {
        try {
            entity.persistAndFlush();
        } catch (PersistenceException e) {
            Throwable rootCause = e.getCause();
            if (rootCause instanceof JdbcSQLIntegrityConstraintViolationException) {
                throw new ResourceAlreadyExists("User already exists");
            }
            throw new ApplicationException(rootCause.getMessage());
        } catch (Exception e) {
            var log = LoggerFactory.getLogger(CreateUpdate.class);
            log.error(e.getMessage(), e.getCause());
        }
    }

}
