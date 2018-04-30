package models;

import play.data.validation.Constraints;

import javax.persistence.Entity;



/**
 * Vendor entity managed by Ebean
 */
@Entity 
public class Vendor extends BaseModel {

    private static final long serialVersionUID = 1L;

    @Constraints.Required
    public String name;

}

