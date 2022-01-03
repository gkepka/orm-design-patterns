package orm.schema;

import orm.annotations.OneToOne;
import orm.session.Executor;
import orm.sql.CommandType;
import orm.sql.Query;
import orm.sql.QueryBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SchemaCreator {
    private List<Query> queries = new ArrayList<>();
    private Executor executor = new Executor(
            "jdbc:mysql://mysql.agh.edu.pl:3306",
            "pmakare1",
            "kmjUCKSydDxdZ022",
            "pmakare1");
    private ClassFinder finder = new ClassFinder();
    private ClassScanner scanner = new ClassScanner();
    private Set<Class> entityClasses;

    public void createSchema() throws Exception {
        entityClasses = finder.findEntityClasses();
        addQueriesToCreateTables();
        addQueriesForOneToOnes();

        for (Query q: queries){
            System.out.println(q);
        }

        // przy strategii Class Table Inheritance nie potrzeba tu dodatkowego kodu do obsługi dziedziczenia
        executor.execute(queries);
    }
    public void addQueriesToCreateTables(){
        for (Class clazz: entityClasses){
            List<Field> columns = scanner.getColumns(clazz);
            QueryBuilder queryBuilder = new QueryBuilder(CommandType.CREATE).addTable(clazz);
            columns.forEach(queryBuilder::addColumn);
            queries.add(queryBuilder.build());
        }
    }
    public void addQueriesForOneToOnes(){
        for (Class clazz: entityClasses){
            QueryBuilder queryBuilder = new QueryBuilder(CommandType.ALTER).addTable(clazz);
            List<Field> fields = scanner.getOneToOneFields(clazz)
                    .stream()
                    .filter(f -> f.getAnnotation(OneToOne.class).foreignKeyInThisTable())
                    .toList();
            if (fields.size() > 0) {
                fields.forEach(queryBuilder::addForeignKey);
                queries.add(queryBuilder.build());
            }
        }
    }
}

//        for (Annotation annotation : classAnnotations) {
//            if (annotation.annotationType() == Entity.class) {
//                QueryBuilder queryBuilder = new QueryBuilder();
//                Query query = queryBuilder.setCommandType(CommandType.CREATE)
//                        .addTable(currentClass)
//                        .build();
//                executor.execute(query);
//            }
//            // TODO: pozostałe adnotacje, jeżeli będą
//        }
//        Field[] fields = currentClass.getDeclaredFields();
//        for (Field field : fields) {
//            Annotation[] fieldAnnotations = field.getAnnotations();
//            for (Annotation annotation : fieldAnnotations) {
//                Class annotationType = annotation.annotationType();
//                // TODO: tworzenie relacji między klasami
//                if (annotationType == Column.class) {
//                    // TODO: Dodanie kolumn z klas nadrzędnych
//                    QueryBuilder queryBuilder = new QueryBuilder();
//                    Query query = queryBuilder.setCommandType(CommandType.ALTER)
//                            .addTable(currentClass)
//                            .addColumn(field)
//                            .build();
//                    executor.execute(query);
//                } else if (annotationType == Id.class) {
//
//                } else if (annotationType == ManyToMany.class) {
//
//                } else if (annotationType == ManyToOne.class) {
////                    if (!classes.contains(getElementClass(field))) {
////                        // kolekcja nie zawiera typu oznaczonego adnotacją @Entity
////                    }
//                } else if (annotationType == OneToMany.class) {
//
//                } else if (annotationType == OneToOne.class) {
////                    if (!classes.contains(field.getType())) {
////                        // brak @Entity przy klasie, z którą ma być relacja - błąd
////                    }
//                    // buidler.addOneToOne(cl, field.getType());
//                }
//            }
//        }

