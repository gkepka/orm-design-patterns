package orm.test;

import orm.annotations.*;
import orm.annotations.OneToOne;

import java.util.ArrayList;
import java.util.List;

@Entity
public class SimpleClass {
    @Id
    public int id;

    @ManyToMany(tableName = "tabelaLacznikowa")
    public List<ManyToManyCl> mtm = new ArrayList<>();

}
