package models;

import play.data.validation.Constraints;

import javax.persistence.Entity;



/**
 * Department entity managed by Ebean
 */
@Entity 
public class Department extends BaseModel {

    private static final long serialVersionUID = 1L;

    @Constraints.Required
    public String name;

}

