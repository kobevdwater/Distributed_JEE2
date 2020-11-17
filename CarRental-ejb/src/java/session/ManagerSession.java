package session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
        try {
            // Heb dit van het internet, misschien op deze manier?
            //TypedQuery<CarType> query = em.createQuery(
            //    "SELECT CarType type FROM Comapny c WHERE c = ?1", CarType.class);
            //List<CarType> employees = query.setParameter("1", company).getResultList();

            
            CarRentalCompany crc = em.find(CarRentalCompany.class, company);
            return crc.getAllTypes();
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public Set<Integer> getCarIds(String company, String type) {
        Set<Integer> out = new HashSet<Integer>();
        
        try {
            //for(Car c: RentalStore.getRental(company).getCars(type)){
            //    out.add(c.getId());
            //}
            
           CarRentalCompany crc = em.find(CarRentalCompany.class, company);
           for (Car c: crc.getCars(type)){
               out.add(c.getId());
           }
          
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        return out;
    }

    @Override
    public int getNumberOfReservations(String company, String type) {
        int result = 0;
        try {
            CarRentalCompany crc = em.find(CarRentalCompany.class, company);
            for (Car car : crc.getCars(type)){
                result += car.getReservations().size();
            }
            return result;
            // return RentalStore.getRental(company).getCar(id).getReservations().size();
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }

    @Override
    public int getNumberOfReservations(String company, String type, int id) {
        try {
            CarRentalCompany crc = em.find(CarRentalCompany.class, company);
            for(Car c: crc.getCars(type)){
                if (c.getId() == id) {
                    return c.getReservations().size();
                }
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
        return 0;
    }
    
    @Override
    public int getNumberOfReservationsBy(String clientname) {
        try {
            
        } catch (IllegalArgumentException e) {
            Logger.getLogger(ManagerSession.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }
    
    @Override
    public Set<String> getBestClients() {
        
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

}