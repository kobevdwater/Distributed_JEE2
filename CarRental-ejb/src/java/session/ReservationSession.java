package session;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.ejb.EJBContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static javax.ejb.TransactionAttributeType.SUPPORTS;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transaction;
import rental.CarRentalCompany;
import rental.CarType;
import rental.Quote;
import rental.RentalStore;
import rental.Reservation;
import rental.ReservationConstraints;
import rental.ReservationException;

@TransactionAttribute(SUPPORTS)
@Stateful
public class ReservationSession implements ReservationSessionRemote {
    

    private EJBContext context;
    private String renter;
    private List<Quote> quotes = new LinkedList<Quote>();
    
    @PersistenceContext
    private EntityManager em;

    @Override
    public Set<String> getAllRentalCompanies() {
        //return new HashSet<String>(RentalStore.getRentals().keySet());
        
        
        // Die maakt een list van crc's maar normaal gezien zou ik toch strings moeten krijgen? select c.name, dus uhhh
        HashSet<String> result = new HashSet();
        List<CarRentalCompany> out = em.createQuery("SELECT c.name FROM CarRentalCompany c", CarRentalCompany.class).getResultList();
        for(CarRentalCompany crc : out) {
            result.add(crc.getName());
        }
        return result;
    }
    
    
    @Override
    public List<CarType> getAvailableCarTypes(Date start, Date end) {
        List<CarType> availableCarTypes = new LinkedList<CarType>();
        for(String crc : getAllRentalCompanies()) {
            for(CarType ct : em.find(CarRentalCompany.class, crc).getAvailableCarTypes(start, end)) {
                if(!availableCarTypes.contains(ct))
                    availableCarTypes.add(ct);
            }
        }
        return availableCarTypes;
    }
    
     public String getCheapestCarType(Date start, Date end, String region) {
         
     }

    @Override
    public Quote createQuote(String company, ReservationConstraints constraints) throws ReservationException {
        try {
            Quote out = em.find(CarRentalCompany.class, company).createQuote(constraints, renter);
            quotes.add(out);
            return out;
        } catch(Exception e) {
            throw new ReservationException(e);
        }
    }
    
    @Override
    public Quote createQuote(ReservationConstraints constraints) throws ReservationException {
        for (String crcName : getAllRentalCompanies()){
            CarRentalCompany crc = em.find(CarRentalCompany.class, crcName);
            if (crc.canCreateQuote(constraints)){
                Quote out = crc.createQuote(constraints, renter);
                quotes.add(out);
                return out;
            }
        }
        throw new ReservationException("could not make the reservation.");
    }

    @Override
    public List<Quote> getCurrentQuotes() {
        return quotes;
    }

    @Override
    @TransactionAttribute(REQUIRED)
    public List<Reservation> confirmQuotes() throws ReservationException {
        List<Reservation> done = new LinkedList<Reservation>();
        try {
            for (Quote quote : quotes) {
                //done.add(RentalStore.getRental(quote.getRentalCompany()).confirmQuote(quote));
                done.add(em.find(CarRentalCompany.class, quote.getRentalCompany()).confirmQuote(quote));
            }
        } catch (Exception e) {
            for(Reservation r:done)
                em.find(CarRentalCompany.class, r.getRentalCompany()).cancelReservation(r);
            context.setRollbackOnly();
            throw new ReservationException(e);
        }
        return done;
    }

    @Override
    public void setRenterName(String name) {
        if (renter != null) {
            throw new IllegalStateException("name already set");
        }
        renter = name;
    }

    @Override
    public String getRenterName() {
        return renter;
    }
}