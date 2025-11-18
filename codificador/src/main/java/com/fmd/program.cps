// Clase base
class Animal {
  let name: string;
  let age: integer;

  function constructor(name: string, age: integer) {
    this.name = name;
    this.age = age;
  }

  function speak(): string {
    return this.name + " makes a sound";
  }
}

// Subclase - NO redeclara name/age
class Dog : Animal {
  let breed: string;
  let name:string;
  let age:integer;

  function constructor(name: string, age: integer, breed: string) {
    this.name = name;
    this.age = age;
    this.breed = breed;
  }

  function bark(): string {
    return this.name + " says Woof!";
  }
}

// Prueba
let myDog: Dog;
myDog = new Dog("Max", 3, "Golden");

//print(myDog.bark());      // Método propio
//print(myDog.speak());     // Método heredado
print(myDog.name);        // Propiedad heredada accesible