import { BehaviorSubject, Observable } from "rxjs";

export class NamedItemStorage<T extends {name: string}> {

  private behaviorSubject: BehaviorSubject<Map<string, T>>;
  private _observable: Observable<Map<string, T>>;
  public get observable(): Observable<Map<string, T>> {
    return this._observable;
  }

  constructor(private map: Map<string, T>) {
    this.behaviorSubject = this._observable = new BehaviorSubject(map);
  }

  currentValue(): Map<string, T> {
    return this.behaviorSubject.value;
  }

  getItem(key: string): T | undefined {
    return this.behaviorSubject.value.get(key);
  }

  setItems(items: Map<string, T>): void {
    this.behaviorSubject.next(items);
  }

  saveItem(item: T): void {
    const itemMap = this.behaviorSubject.value;
    itemMap.set(item.name, item);
    this.behaviorSubject.next(itemMap)
  }

  deleteItem(key: string): void {
    const itemMap = this.behaviorSubject.value;
    itemMap.delete(key);
    this.behaviorSubject.next(itemMap);
  }

  renameItem(oldName: string, newName: string): void {
    const item = this.getItem(oldName);
    if (!item) throw new Error(`Tried to rename nonexistent item ${oldName}`);
    item.name = newName;
    this.saveItem(item);
    this.deleteItem(oldName);
  }

  hasItem(key: string): boolean {
    return this.behaviorSubject.value.has(key);
  }
}
