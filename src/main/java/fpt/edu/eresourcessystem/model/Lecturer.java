package fpt.edu.eresourcessystem.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lecturer extends Account{
    @Id
    private String accountId;  // Reference to Account
    private List<String> managedCourses;
}
