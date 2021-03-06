package orm.sql;

import orm.schema.columns.ForeignKeyColumn;
import orm.utils.SqlTypes;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

public class QueryBuilder {
    private final Query query = new Query();

    public QueryBuilder(){}
    public QueryBuilder(CommandType commandType){
        this.setCommandType(commandType);
    }

    public QueryBuilder setCommandType(CommandType commandType) {
        query.commandType = commandType;
        return this;
    }

    public QueryBuilder addTable(Class<?> cl) {
        if (query.commandType == null) {
            throw new IllegalStateException("Command type not set");
        }
        query.tables.add(cl.getSimpleName().toLowerCase());
        return this;
    }

    public QueryBuilder addTable(String table) {
        if (query.commandType == null) {
            throw new IllegalStateException("Command type not set");
        }
        query.tables.add(table);
        return this;
    }

    public QueryBuilder addColumn(Field field) {
        if (query.commandType == null) {
            throw new IllegalStateException("Command type not set");
        }
        if (query.commandType == CommandType.CREATE || query.commandType == CommandType.ALTER) {
            String type = SqlTypes.getType(field.getType());
            query.columns.add(field.getName().toLowerCase() + " " + type);
        }
        if (query.commandType == CommandType.SELECT || query.commandType == CommandType.INSERT) {
            query.columns.add(field.getName().toLowerCase());
        }
        return this;
    }
    public QueryBuilder addColumn(String column){
        return this.addColumn(column, null);
    }

    public QueryBuilder addColumn(String column, String type) {
        if (query.commandType == null) {
            throw new IllegalStateException("Command type not set");
        }
        if (query.commandType == CommandType.CREATE || query.commandType == CommandType.ALTER) {
            query.columns.add(column + " " + type);
        }
        if (query.commandType == CommandType.SELECT || query.commandType == CommandType.INSERT) {
            query.columns.add(column);
        }
        return this;
    }

    public QueryBuilder addId(){
        query.columns.add("id INT NOT NULL PRIMARY KEY");
        return this;
    }

    public QueryBuilder addForeignKeyConstraint(String column, String referencedTable, String constraintName){
        //this.addColumn(column, "INT");
        query.foreignKeys.add("CONSTRAINT "+constraintName+" FOREIGN KEY ("+column+") REFERENCES "+referencedTable+"(id)");
        return this;
    }

    public QueryBuilder addForeignKeyConstraint(Field field, String tableName){
        String column = field.getName().toLowerCase() + "_id";
        Class<?> clazz = field.getType();
        String refTable;
        // if field is a collection, take inside type
        if (Collection.class.isAssignableFrom(clazz)){
            ParameterizedType wrapperType = (ParameterizedType) field.getGenericType();
            refTable = Arrays.stream(
                        wrapperType
                            .getActualTypeArguments()[0]
                            .getTypeName()
                            .split("\\."))
                    .reduce((first, second) -> second)
                    .orElse(null);
            if (refTable != null)
                    refTable = refTable.toLowerCase();
        }
        else refTable = clazz.getSimpleName().toLowerCase();
        addForeignKeyConstraint(column, refTable, ForeignKeyColumn.getConstraintName(tableName, column, refTable));
        return this;
    }

    public QueryBuilder addCondition(String condition) {
        if (query.commandType == null) {
            throw new IllegalStateException("Command type not set");
        }
        query.conditions.add(condition);
        return this;
    }

    public QueryBuilder addValue(Object value) {
        if (query.commandType == null) {
            throw new IllegalStateException("Command type not set");
        }
        query.values.add(value);
        return this;
    }

    public QueryBuilder setDropColumn(String dropColumn){
        query.dropColumn = dropColumn;
        return this;
    }
    public QueryBuilder setDropForeignKey(String dropForeignKey){
        query.dropForeignKey = dropForeignKey;
        return this;
    }

    public QueryBuilder setForeignKeyChecks(int foreignKeyChecks) {
        if (query.commandType == null) {
            throw new IllegalStateException("Command type not set");
        }
        query.foreignKeyChecks = foreignKeyChecks;
        return this;
    }

    public QueryBuilder distinct(boolean distinct) {
        if (query.commandType == null) {
            throw new IllegalStateException("Command type not set");
        }
        if (query.commandType != CommandType.SELECT) {
            throw new IllegalStateException("Command is not set to SELECT");
        }
        query.distinct = distinct;
        return this;
    }

    public QueryBuilder groupByColumn(String column) {
        if (query.commandType == null) {
            throw new IllegalStateException("Command type not set");
        }
        if (query.commandType != CommandType.SELECT) {
            throw new IllegalStateException("Command is not set to SELECT");
        }
        query.groupByColumns.add(column);
        query.isGrouped = true;
        return this;
    }

    public QueryBuilder addAggregate(AggregateFunction function, String column) {
        if (query.commandType == null) {
            throw new IllegalStateException("Command type not set");
        }
        if (query.commandType != CommandType.SELECT) {
            throw new IllegalStateException("Command is not set to SELECT");
        }
        if (!query.isGrouped) {
            throw new IllegalStateException("No GROUP BY statement");
        }
        query.aggregateFunctions.add(new AbstractMap.SimpleEntry<>(function, column));
        return this;
    }

    public QueryBuilder setColumn(String column, Object value) {
        if (query.commandType == null) {
            throw new IllegalStateException("Command type not set");
        }
        if (query.commandType != CommandType.UPDATE) {
            throw new IllegalStateException("Command is not set to UPDATE");
        }
        query.setColumns.add(column + " = ?");
        query.values.add(value);
        return this;
    }

    public QueryBuilder setColumn(Field column, Object value) {
        if (query.commandType == null) {
            throw new IllegalStateException("Command type not set");
        }
        if (query.commandType != CommandType.UPDATE) {
            throw new IllegalStateException("Command is not set to UPDATE");
        }
        query.setColumns.add(column.getName().toLowerCase() + "= ?");
        query.values.add(value);
        return this;
    }

    public Query build() {
        return query;
    }
}
