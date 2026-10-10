package edu.cmu.cs214.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cmu.cs214.booking.domain.Room;
import edu.cmu.cs214.booking.domain.TimeInterval;
import edu.cmu.cs214.booking.domain.User;
import edu.cmu.cs214.booking.repo.InMemoryBookingStore;
import org.junit.jupiter.api.Test;

class CancelBookingTest {

    private final Room roomA = new Room("A", "Alpha", 10);
    private final User alice = new User("u1", "Alice");
    private final User bob = new User("u2", "Bob");

    private BookingService newService() {
        return new BookingService(new InMemoryBookingStore());
    }

    /** S1: cancelling a confirmed booking removes exactly that booking. */
    @Test
    void cancelRemovesOnlyThatBooking() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(600, 660));
        svc.book(roomA, bob, new TimeInterval(660, 720));
        String id = svc.listBookings(roomA).get(0).id();

        svc.cancelBooking(id);

        assertEquals(1, svc.listBookings(roomA).size());
        assertEquals(bob.id(), svc.listBookings(roomA).get(0).user().id());
    }

    /** S2: cancelling an unknown id changes nothing. */
    @Test
    void cancelUnknownIdDoesNothing() {
        BookingService svc = newService();
        svc.book(roomA, alice, new TimeInterval(600, 660));

        svc.cancelBooking("no-such-booking");

        assertEquals(1, svc.listBookings(roomA).size());
        assertEquals(alice.id(), svc.listBookings(roomA).get(0).user().id());
    }
}
