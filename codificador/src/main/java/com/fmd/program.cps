// Global constants and variables
const PI: integer = 314;
let greeting: string = "Hello, Compiscript!";
let flag: boolean;
let numbers: integer[] = [1, 2, 3, 4, 5];
let matrix: integer[][] = [[1, 2], [3, 4]];

// Simple closure-style function (no nested type signatures)
function makeAdder(x: integer): integer {
  return x + 5;
}

let addFive: integer = (makeAdder(5));
print("5 + 1 = " + addFive);

// Control structures
if (addFive > 5) {
  print("Greater than 5");
} else {
  print("5 or less");
}

while (addFive < 10) {
  addFive = addFive + 1;
}

do {
  print("Result is now " + addFive);
  addFive = addFive - 1;
} while (addFive > 7);

for (let i: integer = 0; i < 3; i = i + 1) {
  print("Loop index: " + i);
}

// Switch-case structure
switch (addFive) {
  case 7:
    print("It's seven");
  case 6:
    print("It's six");
  default:
    print("Something else");
}


// Try-catch structure
try {
  let risky: integer = numbers[10];
  print("Risky access: " + risky);
} catch (err) {
  print("Caught an error: " + err);
}

// Class definition and usage
class Animal {
  let name: string = "hugo";
  let casa: string;

  function constructor(name: string) {
    this.name = name;
  }

  function speak(): string {
    return this.name + " makes a sound.";
  }
}

class Dog : Animal {
  let owner: string;
}

class Cat {
  let jugar: string;
}




