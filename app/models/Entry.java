package models;

import play.data.format.Formats;
import play.data.validation.Constraints;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.Date;

/**
 * Entry entity managed by Ebean
 */
@Entity 
public class Entry extends BaseModel {

    private static final long serialVersionUID = 1L;

    @Constraints.Required
    public String name;
    
    @Formats.DateTime(pattern="yyyy-MM-dd")
    public Date start_date;
    
    @Formats.DateTime(pattern="yyyy-MM-dd")
    public Date end_date;
    
    @ManyToOne
    public Company company;
    
}

