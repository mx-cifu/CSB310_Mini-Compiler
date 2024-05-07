//10 through 100
for (i = 10; i <= 100; i = i + 1) {
    if (i % 5 == 0) {
        print(i);
    } else /*if i is not % 5*/ {
        print(i, "is not divisible by 5");
    }
}
print("All done.");