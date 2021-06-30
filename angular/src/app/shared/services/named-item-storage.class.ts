import { BehaviorSubject, Observable } from "rxjs";

export class NamedItemStorage<T extends {name: string}> {

  private behaviorSubject: BehaviorSubject<Map<string, T>>;
  private _observable: Observable<Map<string, T>>;
  public get observable(): Observable<Map<string, T>> {
    return this._observable;
  }

  constructor(private key: string) {
    const json = window.localStorage.getItem(key);
    const parsed = json ? JSON.parse(json) as unknown : [];
    const array = Array.isArray(parsed) ? parsed : [];
    this.behaviorSubject = this._observable = new BehaviorSubject(new Map<string, T>(array));
  }

  currentValue(): Map<string, T> {
    return this.behaviorSubject.value;
  }

  updateItems(): void {
    const json = JSON.stringify(Array.from(this.behaviorSubject.value));
    window.localStorage.setItem(this.key, json);
  }

  setItems(items: Map<string, T>): void {
    this.behaviorSubject.next(items);
    this.updateItems();
  }

  saveItem(item: T): void {
    const itemMap = this.behaviorSubject.value;
    itemMap.set(item.name, item);
    this.behaviorSubject.next(itemMap)
    this.updateItems();
  }

  deleteItem(key: string): void {
    const itemMap = this.behaviorSubject.value;
    itemMap.delete(key);
    this.behaviorSubject.next(itemMap);
    this.updateItems();
  }

  renameItem(oldName: string, newName: string): void {
    const item = this.currentValue().get(oldName);
    if (!item) throw new Error(`Tried to rename nonexistent item ${oldName}`);
    item.name = newName;
    this.saveItem(item);
    this.deleteItem(oldName);
    this.updateItems();
  }

  hasItem(key: string): boolean {
    return this.behaviorSubject.value.has(key);
  }
}
