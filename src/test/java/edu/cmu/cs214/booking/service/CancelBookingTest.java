package edu.cmu.cs214.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.cmu.cs214.booking.domain.Booking;
import edu.cmu.cs214.booking.domain.Room;
import edu.cmu.cs214.booking.domain.TimeInterval;
import edu.cmu.cs214.booking.domain.User;
import edu.cmu.cs214.booking.repo.InMemoryBookingStore;
import org.junit.jupiter.api.Test;

class CancelBookingTest {

    private final Room roomA = new Room("A", "Alpha", 10);
    private final User alice = new User("u1", "Alice");
    private final User bob = new User("u2", "Bob");
    private final User carol = new User("u3", "Carol");
    private final User dave = new User("u4", "Dave");

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

    /** S3: cancelling promotes the freed slot's waiter into a confirmed booking. */
    @Test
    void cancelPromotesWaitlistedUser() {
        InMemoryBookingStore store = new InMemoryBookingStore();
        BookingService svc = new BookingService(store);
        svc.book(roomA, alice, new TimeInterval(600, 660));
        svc.book(roomA, bob, new TimeInterval(600, 660)); // waitlisted: slot taken
        String aliceBookingId = svc.listBookings(roomA).get(0).id();

        svc.cancelBooking(aliceBookingId);

        assertEquals(1, svc.listBookings(roomA).size());
        Booking promoted = svc.listBookings(roomA).get(0);
        assertEquals(bob.id(), promoted.user().id());
        assertEquals(new TimeInterval(600, 660), promoted.interval());
        assertEquals(0, store.waitlistForRoom(roomA).size());
    }

    /** S4: a waiter whose interval is still blocked is skipped for a later one that fits. */
    @Test
    void cancelSkipsBlockedWaiterForNextOne() {
        InMemoryBookingStore store = new InMemoryBookingStore();
        BookingService svc = new BookingService(store);
        svc.book(roomA, alice, new TimeInterval(600, 660));
        svc.book(roomA, carol, new TimeInterval(700, 760));
        svc.book(roomA, bob, new TimeInterval(640, 720)); // waitlisted: overlaps both bookings
        svc.book(roomA, dave, new TimeInterval(600, 640)); // waitlisted: overlaps alice only
        String aliceBookingId = svc.listBookings(roomA).get(0).id();

        svc.cancelBooking(aliceBookingId);

        assertEquals(2, svc.listBookings(roomA).size());
        assertEquals(carol.id(), svc.listBookings(roomA).get(0).user().id());
        assertEquals(dave.id(), svc.listBookings(roomA).get(1).user().id());
        assertEquals(1, store.waitlistForRoom(roomA).size());
        assertEquals(bob.id(), store.waitlistForRoom(roomA).get(0).user().id());
    }

    /** S5: at most one waiter is promoted per cancellation. */
    @Test
    void cancelPromotesAtMostOneWaiter() {
        InMemoryBookingStore store = new InMemoryBookingStore();
        BookingService svc = new BookingService(store);
        svc.book(roomA, alice, new TimeInterval(600, 760));
        svc.book(roomA, bob, new TimeInterval(600, 660)); // waitlisted
        svc.book(roomA, carol, new TimeInterval(660, 720)); // waitlisted
        String aliceBookingId = svc.listBookings(roomA).get(0).id();

        svc.cancelBooking(aliceBookingId);

        assertEquals(1, svc.listBookings(roomA).size());
        assertEquals(bob.id(), svc.listBookings(roomA).get(0).user().id());
        assertEquals(1, store.waitlistForRoom(roomA).size());
        assertEquals(carol.id(), store.waitlistForRoom(roomA).get(0).user().id());
    }
}
