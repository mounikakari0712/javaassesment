/*
Programming Test Conference room scheduling. Find the nearest open conference room for a team in which a team can hold its meeting. Given n team members with the floor on which they work and the time they want to meet, and a list of conference rooms identified by their floor and room number as a decimal number, maximum number of people it fits and pairs of times they are open - find the best place for the team to have their meeting. If there is more than one room available that fits the team at the chosen time then the best place is on the floor the closest to where the team works. E.g. rooms.txt 7.11,8,9:00,9:15,14:30,15:00 8.23,6,10:00,11:00,14:00,15:00 8.43,7,11:30,12:30,17:00,17:30 9.511,9,9:30,10:30,12:00,12:15,15:15,16:15 9.527,4,9:00,11:00,14:00,16:00 9.547,8,10;30,11:30,13:30,15:30,16:30,17:30 Input: 5,8,10:30,11:30 # 5 team members, located on the 8th floor, meeting time 10:30 - 11:30 Output: 9.547
Please explain: how you solved the problem and how it would behave based on the different parameters (number of team members, longer meeting times, many rooms with random booking times). 
How would you test the program to ensure it always produced the correct results? 
For extra credit, can you improve the solution to split the meeting across more than one room if say only one room is available for a fraction of the meeting and another room is free later to hold the remainder of the meeting during the set time. If you want to make this more powerful - assume that the number of room splits can happen in proportion to the length of the meeting so that say if a meeting is 8 hrs long then the algorithm could schedule it across say up to 4 rooms if a single room was not available for the whole time. You may code the response in any programming language you like, however our primary DevOps programming languages are: Bash Perl Python Groovy
*/
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class ConferenceRoom {
    // office hours are assumed to be 24 hrs a day
    private static final String officeStart = "00:00";//office start time
    private static final String officeEnd = "00:00";// office end time
    private static final String fileName = "rooms.txt";// input file

    public static void main(String[] args) {
        try{
            List<Room> conferenceRoomsInfo = ProcessInputFile();// process the input file
            Map<LocalDateTime,List<Room>> officeSlots = GetOfficeSlots();// generate empty office slot
            PopulateOfficeSlots(officeSlots,conferenceRoomsInfo);// populate office slots with the processed room information
            boolean cont = true;// used to continue the scheduling
            Scanner sc = new Scanner(System.in);
            while(cont)
            {
                Meeting meeting = GetInput();// take the user input about the meeting to be scheduled
                Map<String,List<LocalDateTime>> availableSlots = new HashMap<>();
                for (LocalDateTime slot : meeting.getSlots()) {// loop through slots required for the meeting to be scheduled
                    String slotResult = GetAvailableNearByRoom(officeSlots,slot,meeting.getCapacity(),meeting.getFloor());// get available nearby rooms for the given slot that can accomdate the meeting

                    List<LocalDateTime> value;
                    if(availableSlots.containsKey(slotResult)) {
                        value = availableSlots.get(slotResult);
                    }
                    else{
                        value = new ArrayList<>();
                    }
                    value.add(slot);
                    availableSlots.put(slotResult,value); // store the response to the map
                }
                Map<Slot,String> results = new TreeMap<>(); // used to sort slots based on the start of the meeting time
                for (Map.Entry<String,List<LocalDateTime>> availableSlot: availableSlots.entrySet()) {
                    List<Slot> mergedSlots = MergeTimeSlots(availableSlot.getValue()); // merge the common slot for a room
                    for (Slot mergedSlot : mergedSlots) {
                        results.put(mergedSlot,availableSlot.getKey()); // store the response to treemap
                    }
                }
                System.out.println("Rooms Available for Meeting at "+new Slot(meeting.start,meeting.end)+
                        " with "+meeting.capacity+" near floor "+Math.round(meeting.floor));
                for (Map.Entry<Slot,String> result: results.entrySet()) {
                    System.out.println("RoomNumber "+result.getValue()+ " - "+result.getKey());
                }
                System.out.print("\nSchedule Another Meeting (Y/N)");
                cont = sc.nextLine().equalsIgnoreCase("y");
            }
        }
        catch(Exception ex)
        {
            System.out.println("Error Occurred while Scheduling the Meeting: "+ex.getMessage());
        }
    }
    // class to store room number and capacity and slots available with 15 minute frequency from the input file
    public static class Room {
        private Float roomNumber;
        private Integer capacity;
        private List<LocalDateTime> slots;

        public Room() {
            roomNumber = 0.00f;
            capacity = 0;
            slots = null;
        }

        public Room(Float roomNumber, Integer capacity, List<LocalDateTime> slots) {
            this.roomNumber = roomNumber;
            this.capacity = capacity;
            this.slots = slots;
        }

        public Room(Float roomNumber, Integer capacity) {
            this.roomNumber = roomNumber;
            this.capacity = capacity;
        }
        public List<LocalDateTime> getSlots() {
            return slots;
        }

        public void setSlots(List<LocalDateTime> slots) {
            this.slots = slots;
        }
        public Float getRoomNumber() {
            return roomNumber;
        }

        public void setRoomNumber(Float roomNumber) {
            this.roomNumber = roomNumber;
        }

        public Integer getCapacity() {
            return capacity;
        }

        public void setCapacity(Integer capacity) {
            this.capacity = capacity;
        }
    }
    // class to store slot start and end date
    public static class Slot implements Comparable<Slot>{
        private LocalDateTime start;
        private LocalDateTime end;

        public Slot(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }

        public LocalDateTime getStart() {
            return start;
        }

        public void setStart(LocalDateTime start) {
            this.start = start;
        }

        public LocalDateTime getEnd() {
            return end;
        }

        public void setEnd(LocalDateTime end) {
            this.end = end;
        }

        @Override
        public String toString() {
            return "("+this.getStart().format(DateTimeFormatter.ofPattern("HH:mm"))+","+
                    this.getEnd().format(DateTimeFormatter.ofPattern("HH:mm"))+")";
        }

        @Override
        public int compareTo(Slot slot) {
            return this.getStart().compareTo(slot.getStart());
        }
    }
    // class to store the meeting request entered by user with the floor number, capcity, meetingtime (start and end ) and slots split into 15 minutes frequency
    public static class Meeting {
        private Float floor;
        private Integer capacity;
        private List<LocalDateTime> slots;
        private LocalDateTime start;
        private LocalDateTime end;

        public Meeting(Float floor, Integer capacity, List<LocalDateTime> slots, LocalDateTime start, LocalDateTime end) {
            this.floor = floor;
            this.capacity = capacity;
            this.slots = slots;
            this.start = start;
            this.end = end;
        }

        public Float getFloor() {
            return floor;
        }

        public void setFloor(Float floor) {
            this.floor = floor;
        }

        public Integer getCapacity() {
            return capacity;
        }

        public void setCapacity(Integer capacity) {
            this.capacity = capacity;
        }

        public List<LocalDateTime> getSlots() {
            return slots;
        }

        public void setSlots(List<LocalDateTime> slots) {
            this.slots = slots;
        }

        public LocalDateTime getStart() {
            return start;
        }

        public void setStart(LocalDateTime start) {
            this.start = start;
        }

        public LocalDateTime getEnd() {
            return end;
        }

        public void setEnd(LocalDateTime end) {
            this.end = end;
        }
    }
    // returns list of conference room information processed from the input file
    public static List<Room> ProcessInputFile() throws Exception
    {
        String roomsData = ReadInputFile();// read all the contents of the file
        String[] rooms = roomsData.split(" "); // split the string with the delimiter 
        List<Room> conferenceRoomInfo = new ArrayList<>();
        for (String room : rooms) {// loop through each room information
            String[] roomInfo = room.split(",");// split room information with a commma
            if(roomInfo.length >=4 && roomInfo.length%2 == 0)// assuming room information will atleast have room number, capacity of the room and pair of available start and end times
            {
                Float roomNumber = Float.parseFloat(roomInfo[0]); // room number in the format of <floornumber>.<roomidentifier>
                Integer capacity = Integer.parseInt(roomInfo[1]);// maximum capacity of the room
                Map<LocalDateTime,Room> conferenceInfo = new HashMap<>();
                List<LocalDateTime> slots = new ArrayList<>();
                for(Integer i= 2; i < roomInfo.length; i = i+2)// loop through rest of the list as a pair of two(incremented by two)
                {
                    List<LocalDateTime> slot = GenerateTimeSlots(roomInfo[i],roomInfo[i+1]);
                    slots.addAll(slot);
                }
                conferenceRoomInfo.add(new Room(roomNumber,capacity,slots));// append the room information object to the list
            }
        }
        return conferenceRoomInfo;// returns the list of rooms
    }
    //returns content of the file as string
    public static String ReadInputFile() throws Exception
    {
        String content = "";
        try
        {
            content = new String ( Files.readAllBytes( Paths.get(fileName) ) ); //read all lines in the file
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new Exception("Error Occurred while reading Conference Information File." + e.getMessage());
        }
        return content;
    }
    // generate and return office slots map with key as timeslot with 15 minute frequency and value as List Room objects
    public static Map<LocalDateTime,List<Room>> GetOfficeSlots() throws Exception
    {
        List<LocalDateTime> officeTimeSlots = GenerateTimeSlots(officeStart,officeEnd);
        Map<LocalDateTime,List<Room>> officeSlots = new HashMap<>();
        for (LocalDateTime officeTimeSlot :
                officeTimeSlots) {
            officeSlots.put(officeTimeSlot,new ArrayList<>());// convert a list of slots to a map with a empty room list
        }
        return officeSlots;
    }
    // populate the office slot with the inputed conference room information
    public static Map<LocalDateTime,List<Room>> PopulateOfficeSlots(Map<LocalDateTime,List<Room>> officeSlots, List<Room> conferenceRoomsInfo)
    {
        for (Room conferenceRoomInfo : conferenceRoomsInfo) {
            for (LocalDateTime slot: conferenceRoomInfo.getSlots()) {
                if(officeSlots.containsKey(slot))
                {
                    Room room = new Room(conferenceRoomInfo.getRoomNumber(),conferenceRoomInfo.getCapacity());
                    List<Room> rooms = officeSlots.get(slot);
                    rooms.add(room);
                    officeSlots.put(slot,rooms);
                }
            }
        }
        return officeSlots;
    }
    // generating 15 minute frequency array for a given time range (start and end) if end is 00:00 then it generates a list for 24hrs range
    public static List<LocalDateTime> GenerateTimeSlots(String start,String end) throws Exception
    {
        LocalDateTime startTime = LocalDate.now().atTime(ProcessStringSlot(start));
        LocalDateTime endTime = LocalDate.now().atTime(ProcessStringSlot(end));
        List<LocalDateTime> timeSlots = new ArrayList<LocalDateTime>();
        if(end.equals("00:00"))
        {
            endTime = LocalDate.now().plusDays(1).atTime(ProcessStringSlot(end));
        }
        if(endTime.isBefore(startTime))
        {
            throw new Exception("Start time cannot be later than End Time give slot ("+start+","+end+")");
        }
        while(startTime.isBefore(endTime))
        {
            timeSlots.add(startTime);
            startTime = startTime.plusMinutes(15);
        }
        return timeSlots;
    }
    //process the timeslot string and formats it to HH:mm formart and then parse it to LocalTime object
    public static LocalTime ProcessStringSlot(String slot) throws Exception {
        String newSlot = slot;
        if(slot.indexOf(':') == 1)
            newSlot = "0"+slot;
        try{
            return LocalTime.parse(newSlot);
        }
        catch (DateTimeParseException ex)
        {
            throw new Exception(slot+ " cannot be parsed to HH:mm format");
        }
    }
    // returns the meeting info entered 
    // takes input from the user and validates it and return a meeting object
    public static Meeting GetInput() throws Exception
    {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter Input In format: <capacity:Integer>,<floor:Integer>,<Meeting Start: String(HH:MM)>,<Meeting End: String(HH:MM)> (Ex: 5,8,10:30,11:30) : ");
        String meetingInput = scanner.nextLine();
        String[] meetingSplit = meetingInput.split(",");
        if(meetingSplit.length != 4)// assuming length of the input is always 4
        {
            throw new Exception("Invalid Input");
        }
        Integer meetingCapacity = Integer.parseInt(meetingSplit[0]);// meeting capacity
        Float meetingFloor = Float.parseFloat(meetingSplit[1]);// meeting floor
        List<LocalDateTime> slots = GenerateTimeSlots(meetingSplit[2],meetingSplit[3]);// 15 minute frequency slots for the meeting period
        LocalDateTime start = LocalDate.now().atTime(ProcessStringSlot((meetingSplit[2])));
        LocalDateTime end = LocalDate.now().atTime(ProcessStringSlot((meetingSplit[3])));
        return new Meeting(meetingFloor,meetingCapacity,slots,start,end);// returns meeting object
    }
    // returns the available near by room for a given timeslot which can accomadate the meeting capacity
    // officeslots - dictonary of available office slots with slot as key and (room and capacity) as value
    // slot - timeslot to get the available nearby room
    //capacity - capacity of the meeting
    //floor - floor of the team
    public static String GetAvailableNearByRoom(Map<LocalDateTime,List<Room>> officeSlots,LocalDateTime slot, Integer capacity, Float floor)
    {
        if(officeSlots.containsKey(slot))
        {
            List<Room> availableSlots = officeSlots.get(slot).stream().filter( s -> s.getCapacity() >= capacity).collect(Collectors.toList());// gets the available list of all rooms that can fit the capacity for the given slot
            Room  nearByRoom = availableSlots.stream().min(Comparator.comparing(i -> Math.abs(i.getRoomNumber() - floor))).orElse(null); // get the nearby room based on the floor the team stays
            if(nearByRoom == null)
            {
                return "N/A";// return request N/A if it is not available
            }
            else
            {
                List<Room> rooms = officeSlots.get(slot);
                rooms.remove(nearByRoom);// remove the selected slot from the office slot as it is no longer available
                officeSlots.put(slot,rooms);
                return Float.toString(nearByRoom.getRoomNumber());// return request available room
            }
        }
        else
        {
            return "N/A";// return request N/A if it is not available 
        }
    }
    // returns the merged timeslots
    // slots - list of slots with 15 minutes frequency
    // Explanation :  if list is [time(9:00),time(11:00),time(11:15)] - it will return [(str(9:00),str(9:15)),(str(11:00),str(11:30))]
    public static List<Slot> MergeTimeSlots(List<LocalDateTime> slots)
    {
        Collections.sort(slots);
        List<Slot> mergedSlots = new ArrayList<>();
        LocalDateTime temp = slots.remove(0);
        LocalDateTime start = temp;
        for (LocalDateTime slot : slots) {
            if(temp.plusMinutes(15).isEqual(slot))
            {
                temp = slot;
            }
            else
            {
                temp = temp.plusMinutes(15);
                mergedSlots.add(new Slot(start,temp));
                start = slot;
                temp = slot;
            }
        }
        temp = temp.plusMinutes(15);
        mergedSlots.add(new Slot(start,temp));
        return mergedSlots;
    }
}