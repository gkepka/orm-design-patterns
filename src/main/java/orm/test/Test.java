package orm.test;

import orm.annotations.Column;
import orm.annotations.Entity;
import orm.annotations.Id;

@Entity
public class Test {
    @Id
    int field1;

    @Column
    String field2;

    Object field3;
}
