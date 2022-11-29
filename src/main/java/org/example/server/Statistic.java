package org.example.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
public class Statistic {
    private int uniqueCount;
    private int duplicateCount;
    private int uniqueTotal;

    public void incrementUniqueCount() {
        uniqueCount++;
    }

    public void incrementDuplicateCount() {
        duplicateCount++;
    }

    public void incrementUniqueTotal() {
        uniqueTotal++;
    }

    public void resetUniqueCount() {
        uniqueCount = 0;
    }

    public void resetDuplicateCount() {
        duplicateCount = 0;
    }

    public String toString() {
        return "Received " + uniqueCount + " unique numbers, " + duplicateCount + " duplicates. Unique total: " + uniqueTotal + "\n";
    }
}
