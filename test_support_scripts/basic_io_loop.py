import sys

print("Enter a String:");
sys.stdout.flush()    
for line in sys.stdin:
    if 'crash' == line.rstrip():
        exit(139)
    else:
        print("You entered: " + line.rstrip())
        sys.stdout.flush()
    print("Enter a String:");    
    sys.stdout.flush()