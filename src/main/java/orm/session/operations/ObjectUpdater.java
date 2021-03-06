package orm.session.operations;

import orm.annotations.ManyToMany;
import orm.annotations.OneToOne;
import orm.schema.reflect.ClassScanner;
import orm.session.Executor;
import orm.sql.CommandType;
import orm.sql.QueryBuilder;
import orm.utils.AssociationTableService;
import orm.utils.IdService;

import javax.sql.rowset.CachedRowSet;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ObjectUpdater {
    private final Set<Object> objectsToUpdate;
    private final Executor executor;
    private final ClassScanner classScanner = new ClassScanner();
    private final IdService idService = new IdService();
    private final ObjectSaver objectSaver;

    public ObjectUpdater(Set<Object> objectsToUpdate, Executor executor, ObjectSaver objectSaver){
        this.objectsToUpdate = objectsToUpdate;
        this.executor = executor;
        this.objectSaver = objectSaver;
    }

    private void updateRecord(Class<?> cl, Object object) {

        try {
            Integer thisId = idService.getObjectId(object);
            if (! classScanner.getColumns(cl).isEmpty()) {
                QueryBuilder qb = new QueryBuilder(CommandType.UPDATE);
                qb.addTable(cl);

                for (Field column : classScanner.getColumns(cl)) {
                    column.setAccessible(true);
                    qb.setColumn(column, column.get(object));
                    qb.addCondition("id = " + thisId);
                    column.setAccessible(false);
                }
                executor.execute(qb.build());
            }

            for (Field field : classScanner.getOneToOneFields(cl)) {
                if (field.getAnnotation(OneToOne.class).foreignKeyInThisTable()) {
                    updateForeignKeyValue(cl, object, thisId, field);
                }
            }

            for (Field field : classScanner.getManyToOneFields(cl)) {
                updateForeignKeyValue(cl, object, thisId, field);
            }

            for (Field field : classScanner.getOneToManyFields(cl)) {
                field.setAccessible(true);
                Collection<?> collection = (Collection<?>) field.get(object);
                if (collection != null) {
                    for (Object obj : collection) {
                        objectSaver.saveObject(obj);
                    }
                }
                field.setAccessible(false);
            }

            for (Field field : classScanner.getManyToManyFields(cl)){
                field.setAccessible(true);
                String tableName = field.getAnnotation(ManyToMany.class).tableName();
                String thisColumn = AssociationTableService.getColumnNameForClass(object.getClass());
                String otherColumn = AssociationTableService.getColumnNameForField(field);

                // select from association table
                QueryBuilder qb2 = new QueryBuilder(CommandType.SELECT);
                qb2.addTable(tableName)
                        .addColumn(otherColumn, "")
                        .addCondition(thisColumn + " = " + thisId);

                CachedRowSet crs = executor.execute(qb2.build()).orElseThrow();

                Set<Integer> otherObjectIdsInDatabase = new HashSet<>();
                while (crs.next()){
                    otherObjectIdsInDatabase.add(crs.getInt(1));
                }

                Object fieldValue = field.get(object);
                Set<?> otherObjectsActual;
                if (fieldValue != null) {
                    otherObjectsActual = new HashSet<>(((Collection<?>) fieldValue));
                } else {
                    otherObjectsActual = new HashSet<>();
                }

                for (Object obj : otherObjectsActual) {
                    objectSaver.saveObject(obj);
                }

                Set<Integer> otherObjectIdsActual = new HashSet<>();
                for (Object o: otherObjectsActual){
                    otherObjectIdsActual.add(idService.getObjectId(o));
                }
//             find records to insert
                Set<Integer> idsToInsert = otherObjectIdsActual.stream()
                                            .filter(id -> !otherObjectIdsInDatabase.contains(id))
                                            .collect(Collectors.toSet());
//             find records to delete
                Set<Integer> idsToDelete = otherObjectIdsInDatabase.stream()
                        .filter(id -> !otherObjectIdsActual.contains(id))
                        .collect(Collectors.toSet());

                for (Integer id: idsToInsert){
                    QueryBuilder qb = new QueryBuilder(CommandType.INSERT);
                    qb.addTable(tableName)
                            .addColumn(thisColumn, "")
                            .addColumn(otherColumn, "")
                            .addValue(thisId)
                            .addValue(id);
                    System.out.println("sdfsdfzzzz");
                    executor.execute(qb.build());
                }

                for (Integer id: idsToDelete){
                    QueryBuilder qb = new QueryBuilder(CommandType.DELETE);
                    qb.addTable(tableName)
                            .addCondition(thisColumn + " = " + thisId)
                            .addCondition(otherColumn + " = " + id);
                    executor.execute(qb.build());
                }
                field.setAccessible(false);
             }


        } catch (IllegalAccessException | SQLException e){
            e.printStackTrace();
        }

    }

    private void updateForeignKeyValue(Class<?> cl, Object object, Integer thisId, Field field) throws IllegalAccessException {
        field.setAccessible(true);
        Object fieldValue = field.get(object);
        objectSaver.saveObject(fieldValue);
        QueryBuilder qb = new QueryBuilder(CommandType.UPDATE);
        qb.addTable(cl);
        String columnName = field.getName() + "_id";
        qb.setColumn(columnName, idService.getObjectId(fieldValue));
        qb.addCondition("id = " + thisId);
        executor.execute(qb.build());
        field.setAccessible(false);
    }

    public void updateAll() {
        for (Object object : objectsToUpdate){
            updateRecord(object.getClass(), object);
            for (Class<?> clazz : classScanner.getParentEntityClasses(object.getClass()))
                updateRecord(clazz, object);
        }
    }

}
