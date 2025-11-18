let a: integer = 5;
let b: integer = 7;
let c: integer = a + b;
print(c + "\n");

let d: integer = a * b;
print(d);

function cuadrado(x: integer): integer {
  return x * x;
}

let r: integer = cuadrado(6);
print(r);

let n: integer = 5;

if (n > 0) {
    let i: integer = 1;
    while (i <= n) {
        print(i);
        i = i + 1;
    }
} else {
    print(0);
}

function makeAdder(x: integer): integer {
  return x + 2;
}

let addFive: integer = (makeAdder(3));

while (addFive < 7) {
  // Control structures
  if (addFive > 5) {
    print("Greater than 5 \n");
  } else {
    print("5 or less \n");
  }
  addFive = addFive + 1;
}

function factorial(n: integer): integer {

    if (n <= 1) {

        return 1;
    }
    return n * factorial(n - 1);
}

let result: integer = factorial(5);

print(result); // Debería imprimir 120


