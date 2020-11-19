package session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import rental.Car;
import rental.CarRentalCompany;
import rental.CarType;
import rental.RentalStore;
import rental.Reservation;

@Stateless

public class ManagerSession implements ManagerSessionRemote {
    
    @PersistenceContext
    private EntityManager em;
    
    @Override
    public Collection<CarType> getCarTypes(String company) {
            // Heb dit van het internet, misschien op deze manier?
            List<Long> ctids = em.createQuery(
                "SELECT ct.id \n" + 
                "FROM CarRentalCompany AS crc, carrentalCompany_cartype AS cc, CarType As ct \n"+ 
                "WHERE crc.name = ?1 AND cc.carrentalcompany_name = crc.name AND cc.carTypes_Id = ct.id", Long.class).
                    setParameter("1", company).getResultList();
            Set<CarType> result = new HashSet<>();
            System.out.println(ctids);
            for (long id: ctids){
                result.add(em.find(CarType.class,id));
            }
            return result;
    }

    @Override
    public Set<Integer> getCarIds(String company, String type) {
        Set<Integer> out = new HashSet<Integer>();
        
        try {
           List<Integer> carIDs = em.createQuery(
                    "SELECT c.reservations \n" + 
                    "FROM CarRentalCompany AS crc, Car AS c \n" +
                    "WHERE crc.name = ?1 AND c.type = ?2 AND c.id IN "
                            + "(SELECT c.id FROM CarRentalCompany_car AS cc WHERE cc.id = c.id)", Integer.class).
                    setParameter("1", company).setParameter("2", type).getResultList();
           out.addAll(carIDs);
           return out;
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public int getNumberOfReservations(String company, String type) {
        try {
            List<Long> allReservations = em.createQuery(
                   "SELECT c.idd"
                           + " From CarRentalCompany As crc JOIN crc.cars c JOIN c.type tp JOIN c.reservations r "
                           + "WHERE crc.name = ?1 AND tp.name = ?2", Long.class).
                   setParameter("1", company).setParameter("2", type).getResultList();
            System.out.println(allReservations);
            return allReservations.size();
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }
    
//     "SELECT c.idd \n" +
//                    "FROM Car AS c, APP.CARRENTALCOMPANY_CAR AS crcc, CarType AS ct, APP.CAR_RESERVATION AS cr \n" +
//                    "WHERE crcc.carRentalCompany_Name = ?1 AND ct.name = ?2 AND c.TYPE_ID = ct.id AND cr.CAR_IDD = c.idd AND crcc.cars_idd = c.idd"

    @Override
    public int getNumberOfReservations(String company, String type, int id) {
        try {
            List<Reservation> allReservations = em.createQuery(
                    "SELECT c.idd \n" +
                    "FROM CarRentalCompany AS crc, Car AS c \n" +
                    "WHERE crc.name = ?1 AND c.type = ?2 AND c.id IN "
                            + "(SELECT c.id FROM CarRentalCompany_car AS cc WHERE cc.id = c.id AND cc.id = ?3)", Reservation.class).
                    setParameter("1", company).setParameter("2", type).setParameter("3", id).getResultList();
            return allReservations.size();
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }
    
    @Override
    public int getNumberOfReservationsBy(String clientname) {
        int result = 0;
        Set<String> crcs = getAllRentalCompanies();
        for (String crc : crcs){
           result += em.find(CarRentalCompany.class,crc).getReservationsBy(clientname).size();
        }
        return result;
    }
    
    @Override
    public Set<String> getBestClients() {
        Map<String,Integer> amountPerClient = new HashMap<String,Integer>();
        Map<String,Integer> amountPerCrc;
        for (String crc : getAllRentalCompanies()){
            amountPerCrc = em.find(CarRentalCompany.class,crc).getAmountOfReservationsPerRenter();
            for (String renter : amountPerCrc.keySet()){
                amountPerClient.putIfAbsent(renter, 0);
                amountPerClient.replace(renter, amountPerClient.get(renter)+amountPerCrc.get(renter));
            }
        }
        Set<String> result = new HashSet<String>();
        int max = 0;
        for (String renter : amountPerClient.keySet()){
            int amount = amountPerClient.get(renter);
            if (amount > max){
                result.clear();
                result.add(renter);
                max = amount;
            } else if (amount == max) {
                result.add(renter);
            }
        }
        return result;
    }
    
    @Override
    public CarType getMostPopularCarTypeIn(String carRentalCompanyName, int year) {
        CarRentalCompany crc = em.find(CarRentalCompany.class,carRentalCompanyName);
        return crc.getMostPopularCarTypeIn(year);
    }
    

    @Override
    public void addCarRentalCompany(String datafile) {
        try {
            CrcData data = loadData(datafile);
            CarRentalCompany company = new CarRentalCompany(data.name, data.regions, data.cars);
            System.out.println(em.contains(company));
            for (CarType cartype : company.getAllTypes()){
                    em.persist(cartype);
            }
            for (Car car : data.cars){
                em.persist(car);
            }
            em.persist(company);
            Logger.getLogger(RentalStore.class.getName()).log(Level.INFO, "Loaded {0} from file {1}", new Object[]{data.name, datafile});
        } catch (NumberFormatException ex) {
            Logger.getLogger(RentalStore.class.getName()).log(Level.SEVERE, "bad file", ex);
        } catch (IOException ex) {
            Logger.getLogger(RentalStore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static CrcData loadData(String datafile)
            throws NumberFormatException, IOException {

        CrcData out = new CrcData();
        StringTokenizer csvReader;
        int nextuid = 0;
       
        //open file from jar
        BufferedReader in = new BufferedReader(new InputStreamReader(RentalStore.class.getClassLoader().getResourceAsStream(datafile)));
        
        try {
            while (in.ready()) {
                String line = in.readLine();
                
                if (line.startsWith("#")) {
                    // comment -> skip					
                } else if (line.startsWith("-")) {
                    csvReader = new StringTokenizer(line.substring(1), ",");
                    out.name = csvReader.nextToken();
                    out.regions = Arrays.asList(csvReader.nextToken().split(":"));
                } else {
                    csvReader = new StringTokenizer(line, ",");
                    //create new car type from first 5 fields
                    CarType type = new CarType(csvReader.nextToken(),
                            Integer.parseInt(csvReader.nextToken()),
                            Float.parseFloat(csvReader.nextToken()),
                            Double.parseDouble(csvReader.nextToken()),
                            Boolean.parseBoolean(csvReader.nextToken()));
                    //create N new cars with given type, where N is the 5th field
                    for (int i = Integer.parseInt(csvReader.nextToken()); i > 0; i--) {
                        out.cars.add(new Car(nextuid++, type));
                    }        
                }
            } 
        } finally {
            in.close();
        }

        return out;
    }
    
    static class CrcData {
            public List<Car> cars = new LinkedList<Car>();
            public String name;
            public List<String> regions =  new LinkedList<String>();
    }
    
    // methode gecopy-pased van ReservationSession
    public Set<String> getAllRentalCompanies() {
        HashSet<String> result = new HashSet();
        List<String> out = em.createQuery("SELECT c.name FROM CarRentalCompany c", String.class).getResultList();
        result.addAll(out);
        return result;
    }

}