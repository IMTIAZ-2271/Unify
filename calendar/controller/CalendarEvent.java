package com.example.unify.calendar.controller;
import java.time.LocalDate;
import java.time.LocalTime;

public class CalendarEvent {
        private LocalDate date;
        private LocalTime startTime;
        private LocalTime endTime;
        private String description;

        public CalendarEvent(LocalDate date, LocalTime startTime, LocalTime endTime, String description) {
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
            this.description = description;
        }

        public LocalDate getDate() { return date; }
        public LocalTime getStartTime() { return startTime; }
        public LocalTime getEndTime() { return endTime; }
        public String getDescription() { return description; }

        @Override
        public String toString() {
            return startTime + " - " + endTime + ": " + description;
        }
}
