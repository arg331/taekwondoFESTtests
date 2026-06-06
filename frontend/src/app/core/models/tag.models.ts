export interface TagResponse {
  id: number;
  name: string;
  color: string;
  createdAt: string;
}

export interface CreateTagRequest {
  name: string;
  color: string;
}

export interface RenameTagRequest {
  newName: string;
}
