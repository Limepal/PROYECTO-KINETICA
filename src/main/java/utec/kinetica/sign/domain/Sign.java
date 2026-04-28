package utec.kinetica.sign.domain;

import jakarta.persistence.*;
import lombok.Setter;
import lombok.Getter;
import java.util.List;      // Imports the List interface


@Setter
@Getter
@Entity
public class Sign {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private long id;

    private List<String> meanings;

    public Sign(){}

    public Sign(long id, List<String> meanings){
        this.id = id;
        this.meanings = meanings;
    }

}
