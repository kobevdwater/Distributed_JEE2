package rental;

import java.io.Serializable;
import javax.persistence.*;
import static javax.persistence.GenerationType.AUTO;
import static javax.persistence.GenerationType.IDENTITY;

@Entity
public class CarType implements Serializable{

    
    private String name;
    private int nbOfSeats;
    private boolean smokingAllowed;
    private double rentalPricePerDay;
    //trunk space in liters
    private float trunkSpace;
    
    /***************
     * CONSTRUCTOR *
     ***************/
    
    protected CarType(){}
    
    public CarType(String name, int nbOfSeats, float trunkSpace, double rentalPricePerDay, boolean smokingAllowed) {
        this.name = name;
        this.nbOfSeats = nbOfSeats;
        this.trunkSpace = trunkSpace;
        this.rentalPricePerDay = rentalPricePerDay;
        this.smokingAllowed = smokingAllowed;
    }
    
    @Id
    @GeneratedValue(strategy = AUTO) long id;
    


    public String getName() {
    	return name;
    }
    
    protected void setName(String name){
        this.name = name;
    }
    
    public int getNbOfSeats() {
        return nbOfSeats;
    }
    
    protected void setNbOfSeats(int nbOfSeats) {
        this.nbOfSeats = nbOfSeats;
    }
    
    public boolean isSmokingAllowed() {
        return smokingAllowed;
    }
    
    protected void setSmokingAllowed(boolean sm) {
        this.smokingAllowed = sm;
    }

    public double getRentalPricePerDay() {
        return rentalPricePerDay;
    }
    
    protected void setRentalPricePerDay(double rp) {
        this.rentalPricePerDay = rp;
    }
    
    public float getTrunkSpace() {
    	return trunkSpace;
    }
    
    protected void setTrunkSpace(float ts) {
        this.trunkSpace = ts;
    }
    
    /*************
     * TO STRING *
     *************/
    
    @Override
    public String toString() {
    	return String.format("Car type: %s \t[seats: %d, price: %.2f, smoking: %b, trunk: %.0fl]" , 
                getName(), getNbOfSeats(), getRentalPricePerDay(), isSmokingAllowed(), getTrunkSpace());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
	int result = 1;
	result = prime * result + ((name == null) ? 0 : name.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
	if (obj == null)
            return false;
	if (getClass() != obj.getClass())
            return false;
	CarType other = (CarType) obj;
	if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
	return true;
    }
}