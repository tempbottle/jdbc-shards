/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package com.suning.snfddal.dbobject.table;

import java.util.ArrayList;
import java.util.List;

import com.suning.snfddal.command.ddl.CreateTableData;
import com.suning.snfddal.engine.Database;
import com.suning.snfddal.engine.DbSettings;
import com.suning.snfddal.util.StatementBuilder;
import com.suning.snfddal.util.StringUtils;

/**
 * The base class of a regular table, or a user defined table.
 *
 * @author Thomas Mueller
 * @author Sergi Vladykin
 */
public abstract class TableBase extends Table {

    /**
     * The table engine used (null for regular tables).
     */
    private final String tableEngine;
    /** Provided table parameters */
    private List<String> tableEngineParams = new ArrayList<String>();

    private final boolean globalTemporary;

    public TableBase(CreateTableData data) {
        super(data.schema, data.id, data.tableName,
                data.persistIndexes, data.persistData);
        this.tableEngine = data.tableEngine;
        this.globalTemporary = data.globalTemporary;
        if (data.tableEngineParams != null) {
            this.tableEngineParams = data.tableEngineParams;
        }
        setTemporary(data.temporary);
        Column[] cols = new Column[data.columns.size()];
        data.columns.toArray(cols);
        setColumns(cols);
    }

    @Override
    public String getDropSQL() {
        return "DROP TABLE IF EXISTS " + getSQL() + " CASCADE";
    }

    @Override
    public String getCreateSQL() {
        Database db = getDatabase();
        if (db == null) {
            // closed
            return null;
        }
        StatementBuilder buff = new StatementBuilder("CREATE ");
        if (isTemporary()) {
            if (isGlobalTemporary()) {
                buff.append("GLOBAL ");
            } else {
                buff.append("LOCAL ");
            }
            buff.append("TEMPORARY ");
        } else if (isPersistIndexes()) {
            buff.append("CACHED ");
        } else {
            buff.append("MEMORY ");
        }
        buff.append("TABLE ");
        if (isHidden) {
            buff.append("IF NOT EXISTS ");
        }
        buff.append(getSQL());
        if (comment != null) {
            buff.append(" COMMENT ").append(StringUtils.quoteStringSQL(comment));
        }
        buff.append("(\n    ");
        for (Column column : columns) {
            buff.appendExceptFirst(",\n    ");
            buff.append(column.getCreateSQL());
        }
        buff.append("\n)");
        if (tableEngine != null) {
            DbSettings s = db.getSettings();
            String d = s.defaultTableEngine;
            if (d == null || !tableEngine.endsWith(d)) {
                buff.append("\nENGINE ");
                buff.append(StringUtils.quoteIdentifier(tableEngine));
            }
        }
        if (!tableEngineParams.isEmpty()) {
            buff.append("\nWITH ");
            buff.resetCount();
            for (String parameter : tableEngineParams) {
                buff.appendExceptFirst(", ");
                buff.append(StringUtils.quoteIdentifier(parameter));
            }
        }
        if (!isPersistIndexes() && !isPersistData()) {
            buff.append("\nNOT PERSISTENT");
        }
        if (isHidden) {
            buff.append("\nHIDDEN");
        }
        return buff.toString();
    }

    @Override
    public boolean isGlobalTemporary() {
        return globalTemporary;
    }

}
